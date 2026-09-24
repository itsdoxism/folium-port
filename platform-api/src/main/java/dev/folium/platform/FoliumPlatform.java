package dev.folium.platform;

/**
 * Narrow host boundary between portable client logic and the browser.
 *
 * Minecraft-facing adapters should depend on this API instead of directly
 * touching desktop-native APIs.
 */
public interface FoliumPlatform {
    WindowHost window();

    InputHost input();

    NetworkHost network();

    StorageHost storage();

    AudioHost audio();

    ClockHost clock();
}
