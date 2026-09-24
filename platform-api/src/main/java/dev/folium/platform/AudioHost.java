package dev.folium.platform;

public interface AudioHost {
    void resume();

    void suspend();

    boolean isAvailable();
}
