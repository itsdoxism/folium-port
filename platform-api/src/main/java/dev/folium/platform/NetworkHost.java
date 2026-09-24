package dev.folium.platform;

public interface NetworkHost {
    Connection connect(String endpoint);

    interface Connection {
        boolean isOpen();

        void send(byte[] payload);

        byte[] poll();

        void close();
    }
}
