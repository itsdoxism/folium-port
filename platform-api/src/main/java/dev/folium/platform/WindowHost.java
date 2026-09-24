package dev.folium.platform;

public interface WindowHost {
    int framebufferWidth();

    int framebufferHeight();

    boolean pointerLocked();

    void requestPointerLock();

    void releasePointerLock();

    void setTitle(String title);
}
