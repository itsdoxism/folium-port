package dev.folium.gateway;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FoliumGatewayServer extends WebSocketServer {
    private static final int READ_BUFFER_SIZE = 64 * 1024;

    private final String backendHost;
    private final int backendPort;
    private final Map<WebSocket, Session> sessions =
        new ConcurrentHashMap<>();

    public FoliumGatewayServer(
        InetSocketAddress listenAddress,
        String backendHost,
        int backendPort
    ) {
        super(listenAddress);
        this.backendHost = backendHost;
        this.backendPort = backendPort;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (!"/folium-gateway".equals(handshake.getResourceDescriptor())) {
            conn.close(1008, "Unsupported Folium gateway path");
            return;
        }

        try {
            Session session = new Session(
                conn,
                backendHost,
                backendPort
            );

            sessions.put(conn, session);
            session.start();
        } catch (Exception error) {
            sessions.remove(conn);
            conn.close(1011, "Folium backend connection failed");
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        Session session = sessions.get(conn);

        if (session == null) {
            conn.close(1011, "Folium gateway session missing");
            return;
        }

        try {
            byte[] bytes = new byte[message.remaining()];
            message.get(bytes);
            session.write(bytes);
        } catch (IOException error) {
            conn.close(1011, "Folium TCP write failed");
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        conn.close(1003, "Folium gateway accepts binary frames only");
    }

    @Override
    public void onClose(
        WebSocket conn,
        int code,
        String reason,
        boolean remote
    ) {
        Session session = sessions.remove(conn);
        if (session != null) {
            session.close();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            Session session = sessions.remove(conn);
            if (session != null) {
                session.close();
            }
        }

        ex.printStackTrace(System.err);
    }

    @Override
    public void onStart() {
        System.out.println(
            "Folium raw TCP gateway listening on ws://" +
                getAddress().getHostString() +
                ":" +
                getAddress().getPort() +
                " -> " +
                backendHost +
                ":" +
                backendPort
        );
    }

    private static final class Session implements AutoCloseable {
        private final WebSocket webSocket;
        private final String backendHost;
        private final int backendPort;
        private final Socket tcpSocket = new Socket();

        private InputStream in;
        private OutputStream out;
        private Thread reader;
        private volatile boolean closed;

        private Session(
            WebSocket webSocket,
            String backendHost,
            int backendPort
        ) {
            this.webSocket = webSocket;
            this.backendHost = backendHost;
            this.backendPort = backendPort;
        }

        void start() throws IOException {
            tcpSocket.connect(
                new InetSocketAddress(backendHost, backendPort),
                10_000
            );

            tcpSocket.setTcpNoDelay(true);
            tcpSocket.setKeepAlive(true);

            in = tcpSocket.getInputStream();
            out = tcpSocket.getOutputStream();

            reader = new Thread(
                this::readLoop,
                "folium-gateway-reader"
            );
            reader.setDaemon(true);
            reader.start();
        }

        synchronized void write(byte[] bytes) throws IOException {
            if (closed) {
                throw new IOException("Folium gateway session closed");
            }

            out.write(bytes);
            out.flush();
        }

        private void readLoop() {
            byte[] buffer = new byte[READ_BUFFER_SIZE];

            try {
                while (!closed) {
                    int read = in.read(buffer);

                    if (read < 0) {
                        break;
                    }

                    if (read == 0) {
                        continue;
                    }

                    byte[] chunk = new byte[read];
                    System.arraycopy(buffer, 0, chunk, 0, read);
                    webSocket.send(chunk);
                }
            } catch (Exception error) {
                if (!closed && webSocket.isOpen()) {
                    webSocket.close(
                        1011,
                        "Folium backend read failed"
                    );
                }
            } finally {
                close();
            }
        }

        @Override
        public void close() {
            closed = true;

            try {
                tcpSocket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
