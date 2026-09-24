package dev.folium.platform;

public interface InputHost {
    boolean keyDown(int keyCode);

    boolean mouseButtonDown(int button);

    double mouseX();

    double mouseY();

    double wheelDeltaX();

    double wheelDeltaY();

    InputEvent pollEvent();

    void clearEvents();
}
