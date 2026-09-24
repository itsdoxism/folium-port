package dev.folium.platform;

public interface NetworkHost {
    Connection connect(String endpoint);

    interface Connection {
        boolean isOpen();

        boolean isClosed();

        void send(byte[] payload);

        byte[] poll();

        void close();
    }
}
