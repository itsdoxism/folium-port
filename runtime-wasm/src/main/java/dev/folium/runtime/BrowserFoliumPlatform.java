package dev.folium.runtime;

import dev.folium.platform.AudioHost;
import dev.folium.platform.ClockHost;
import dev.folium.platform.FoliumPlatform;
import dev.folium.platform.GraphicsHost;
import dev.folium.platform.InputHost;
import dev.folium.platform.NetworkHost;
import dev.folium.platform.StorageHost;
import dev.folium.platform.WindowHost;

public final class BrowserFoliumPlatform implements FoliumPlatform {
    private final GraphicsHost graphics = new BrowserGraphicsHost();
    private final ClockHost clock = new BrowserClockHost();

    private final WindowHost window = new BrowserWindowHost();
    private final InputHost input = new UnsupportedInputHost();
    private final NetworkHost network = endpoint -> {
        throw unsupported("network");
    };
    private final StorageHost storage = new UnsupportedStorageHost();
    private final AudioHost audio = new UnsupportedAudioHost();

    @Override
    public WindowHost window() {
        return window;
    }

    @Override
    public InputHost input() {
        return input;
    }

    @Override
    public GraphicsHost graphics() {
        return graphics;
    }

    @Override
    public NetworkHost network() {
        return network;
    }

    @Override
    public StorageHost storage() {
        return storage;
    }

    @Override
    public AudioHost audio() {
        return audio;
    }

    @Override
    public ClockHost clock() {
        return clock;
    }

    private static UnsupportedOperationException unsupported(String subsystem) {
        return new UnsupportedOperationException(
            "Folium browser " + subsystem + " host is not implemented yet"
        );
    }

    private static final class UnsupportedInputHost implements InputHost {
        @Override public boolean keyDown(int keyCode) { return false; }
        @Override public boolean mouseButtonDown(int button) { return false; }
        @Override public double mouseX() { return 0.0; }
        @Override public double mouseY() { return 0.0; }
        @Override public double wheelDeltaX() { return 0.0; }
        @Override public double wheelDeltaY() { return 0.0; }
    }

    private static final class UnsupportedStorageHost implements StorageHost {
        @Override public byte[] read(String key) { throw unsupported("storage"); }
        @Override public void write(String key, byte[] data) { throw unsupported("storage"); }
        @Override public void delete(String key) { throw unsupported("storage"); }
    }

    private static final class UnsupportedAudioHost implements AudioHost {
        @Override public void resume() { throw unsupported("audio"); }
        @Override public void suspend() { throw unsupported("audio"); }
        @Override public boolean isAvailable() { return false; }
    }
}
