package dev.folium.platform;

/**
 * The narrow host boundary between portable client logic and the environment
 * that actually provides windowing, input, networking, storage and audio.
 *
 * Minecraft classes should eventually depend on adapters around this API
 * instead of directly touching desktop-native APIs.
 */
public interface FoliumPlatform {
    WindowHost window();

    NetworkHost network();

    StorageHost storage();

    AudioHost audio();
}
