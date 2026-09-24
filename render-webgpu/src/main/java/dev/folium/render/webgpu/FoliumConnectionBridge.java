package dev.folium.render.webgpu;

import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.handshake.HandshakeProtocols;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class FoliumConnectionBridge {
    private static final int MAX_PACKETS_PER_TICK = 4096;

    private static final Map<Connection, State> STATES =
        Collections.synchronizedMap(new WeakHashMap<>());

    private FoliumConnectionBridge() {
    }

    public static void connect(
        Connection connection,
        InetSocketAddress address
    ) {
        String endpoint = "minecraft://" +
            address.getHostString() +
            ":" +
            address.getPort();

        FoliumNetworkSession session = new FoliumNetworkSession(endpoint);
        session.setOutboundProtocol(HandshakeProtocols.SERVERBOUND);

        STATES.put(connection, new State(address, session));
    }

    public static void setInboundProtocol(
        Connection connection,
        ProtocolInfo<?> protocol
    ) {
        require(connection).session.setInboundProtocol(protocol);
    }

    public static void setOutboundProtocol(
        Connection connection,
        ProtocolInfo<?> protocol
    ) {
        require(connection).session.setOutboundProtocol(protocol);
    }

    public static void send(
        Connection connection,
        Packet<?> packet
    ) {
        require(connection).session.send(packet);
    }

    public static void setupCompression(
        Connection connection,
        int threshold,
        boolean validateDecompressed
    ) {
        require(connection).session.setupCompression(
            threshold,
            validateDecompressed
        );
    }

    public static void setEncryptionKey(
        Connection connection,
        javax.crypto.Cipher decryptCipher,
        javax.crypto.Cipher encryptCipher
    ) {
        require(connection).session.setEncryptionKey(
            decryptCipher,
            encryptCipher
        );
    }

    public static boolean isConnected(Connection connection) {
        State state = STATES.get(connection);
        return state != null && state.session.isOpen();
    }

    public static boolean isConnecting(Connection connection) {
        State state = STATES.get(connection);
        return state != null &&
            !state.session.isOpen() &&
            !state.session.isClosed();
    }

    public static InetSocketAddress remoteAddress(Connection connection) {
        State state = STATES.get(connection);
        return state == null ? null : state.address;
    }

    public static void tick(Connection connection) {
        State state = STATES.get(connection);
        if (state == null) {
            return;
        }

        PacketListener listener = connection.getPacketListener();
        int handled = 0;

        while (handled < MAX_PACKETS_PER_TICK) {
            Packet<?> packet = state.session.pollPacket();
            if (packet == null) {
                break;
            }

            handled++;

            if (
                listener == null ||
                !listener.isAcceptingMessages() ||
                !listener.shouldHandleMessage(packet)
            ) {
                continue;
            }

            try {
                ((Packet) packet).handle(listener);
            } catch (Exception error) {
                listener.onPacketError(packet, error);
            }
        }

        if (listener instanceof TickablePacketListener tickable) {
            tickable.tick();
        }

        if (state.session.isClosed()) {
            connection.handleDisconnection();
        }
    }

    public static void disconnect(
        Connection connection,
        DisconnectionDetails details
    ) {
        State state = STATES.remove(connection);
        if (state != null) {
            state.session.close();
        }
    }

    private static State require(Connection connection) {
        State state = STATES.get(connection);
        if (state == null) {
            throw new IllegalStateException(
                "Folium Connection has no browser network session"
            );
        }
        return state;
    }

    private record State(
        InetSocketAddress address,
        FoliumNetworkSession session
    ) {
    }
}
