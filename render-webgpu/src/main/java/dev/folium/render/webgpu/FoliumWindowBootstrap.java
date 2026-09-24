package dev.folium.render.webgpu;

import dev.folium.platform.FoliumRuntime;
import com.mojang.blaze3d.platform.Window;

public final class FoliumWindowBootstrap {
    private FoliumWindowBootstrap() {
    }

    public static int framebufferWidth() {
        return Math.max(
            Window.MIN_WINDOW_WIDTH,
            FoliumRuntime.platform().window().framebufferWidth()
        );
    }

    public static int framebufferHeight() {
        return Math.max(
            Window.MIN_WINDOW_HEIGHT,
            FoliumRuntime.platform().window().framebufferHeight()
        );
    }

    public static Window.FramebufferSize queryFramebufferSize() {
        return new Window.FramebufferSize(
            framebufferWidth(),
            framebufferHeight()
        );
    }

    public static void setTitle(String title) {
        FoliumRuntime.platform().window().setTitle(title);
    }

    public static String platformName() {
        return "Web";
    }

    public static long windowHandle() {
        return 1L;
    }
}
