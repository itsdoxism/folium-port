package dev.folium.render.webgpu;

import dev.folium.platform.FoliumRuntime;

import java.net.URI;
import java.nio.file.Path;

public final class FoliumUtilityBridge {
    private FoliumUtilityBridge() {
    }

    public static void openUri(URI uri) {
        if (uri != null) {
            FoliumRuntime.platform().window().openExternal(uri.toString());
        }
    }

    public static void openPath(Path path) {
        if (path != null) {
            openUri(path.toUri());
        }
    }

    public static void error(String message) {
        FoliumRuntime.platform().window().showError(message);
    }

    public static boolean errorWithContinue(String message) {
        return FoliumRuntime.platform().window().showErrorWithContinue(message);
    }
}
