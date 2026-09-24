package dev.folium.runtime;

import dev.folium.platform.NetworkHost;
import org.teavm.jso.JSBody;

public final class BrowserNetworkHost implements NetworkHost {
    @Override
    public Connection connect(String endpoint) {
        int token = open(endpoint);
        return new BrowserConnection(token);
    }

    private static final class BrowserConnection implements Connection {
        private final int token;
        private boolean closed;

        private BrowserConnection(int token) {
            this.token = token;
        }

        @Override
        public boolean isOpen() {
            return !closed && socketOpen(token);
        }

        @Override
        public void send(byte[] payload) {
            if (closed) {
                throw new IllegalStateException("Folium WebSocket is closed");
            }
            sendBinary(token, payload);
        }

        @Override
        public byte[] poll() {
            if (closed) {
                return null;
            }
            return pollBinary(token);
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                closeSocket(token);
            }
        }
    }

    @JSBody(params = {"endpoint"}, script = """
        const state = globalThis.__foliumSockets ||= {
            nextToken: 1,
            sockets: new Map()
        };

        const token = state.nextToken++;
        const queue = [];
        const socket = new WebSocket(endpoint);
        socket.binaryType = 'arraybuffer';

        const entry = {
            socket,
            queue,
            open: false,
            closed: false,
            error: null
        };

        state.sockets.set(token, entry);

        socket.addEventListener('open', () => {
            entry.open = true;
        });

        socket.addEventListener('message', event => {
            if (event.data instanceof ArrayBuffer) {
                queue.push(new Uint8Array(event.data));
                return;
            }

            if (ArrayBuffer.isView(event.data)) {
                queue.push(new Uint8Array(
                    event.data.buffer,
                    event.data.byteOffset,
                    event.data.byteLength
                ).slice());
                return;
            }

            console.warn(
                'Folium ignored non-binary WebSocket message',
                typeof event.data
            );
        });

        socket.addEventListener('close', () => {
            entry.open = false;
            entry.closed = true;
        });

        socket.addEventListener('error', event => {
            entry.error = event;
        });

        return token;
    """)
    private static native int open(String endpoint);

    @JSBody(params = {"token"}, script = """
        const entry = globalThis.__foliumSockets?.sockets?.get(token);
        return !!entry?.open && entry.socket?.readyState === WebSocket.OPEN;
    """)
    private static native boolean socketOpen(int token);

    @JSBody(params = {"token", "payload"}, script = """
        const entry = globalThis.__foliumSockets?.sockets?.get(token);
        if (!entry || entry.socket.readyState !== WebSocket.OPEN) {
            throw new Error('Folium WebSocket is not open');
        }

        const bytes = payload instanceof Uint8Array
            ? payload
            : new Uint8Array(payload.buffer, payload.byteOffset, payload.byteLength);

        entry.socket.send(bytes);
    """)
    private static native void sendBinary(int token, byte[] payload);

    @JSBody(params = {"token"}, script = """
        const entry = globalThis.__foliumSockets?.sockets?.get(token);
        if (!entry || entry.queue.length === 0) return null;
        return entry.queue.shift();
    """)
    private static native byte[] pollBinary(int token);

    @JSBody(params = {"token"}, script = """
        const state = globalThis.__foliumSockets;
        const entry = state?.sockets?.get(token);
        if (!entry) return;

        try {
            entry.socket.close(1000, 'Folium client closed');
        } catch (_) {}

        entry.open = false;
        entry.closed = true;
        state.sockets.delete(token);
    """)
    private static native void closeSocket(int token);
}
