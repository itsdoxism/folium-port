package dev.folium.platform;

public interface StorageHost {
    byte[] read(String key);

    void write(String key, byte[] data);

    void delete(String key);
}
