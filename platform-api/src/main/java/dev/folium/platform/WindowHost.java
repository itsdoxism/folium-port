package dev.folium.platform;

public interface WindowHost {
    int framebufferWidth();

    int framebufferHeight();

    boolean pointerLocked();

    void requestPointerLock();

    void releasePointerLock();

    void setTitle(String title);

    void openExternal(String uri);

    void showError(String message);

    boolean showErrorWithContinue(String message);
}
