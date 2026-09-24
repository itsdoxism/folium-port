package dev.folium.render.webgpu;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import dev.folium.platform.FoliumRuntime;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.List;

public final class FoliumDisplayBootstrap {
    public static final int PRIMARY_DISPLAY_ID = 1;

    private FoliumDisplayBootstrap() {
    }

    public static Monitor primaryMonitor() {
        int width = Math.max(
            1,
            FoliumRuntime.platform().window().framebufferWidth()
        );
        int height = Math.max(
            1,
            FoliumRuntime.platform().window().framebufferHeight()
        );

        VideoMode mode = new VideoMode(
            width,
            height,
            8,
            8,
            8,
            60.0f
        );

        return new Monitor(
            "Folium Browser Display",
            PRIMARY_DISPLAY_ID,
            List.of(mode),
            mode,
            0,
            0,
            width,
            height
        );
    }

    public static Int2ObjectMap<Monitor> monitorMap() {
        Int2ObjectOpenHashMap<Monitor> monitors =
            new Int2ObjectOpenHashMap<>();

        monitors.put(PRIMARY_DISPLAY_ID, primaryMonitor());
        return monitors;
    }

    public static Monitor monitor(int id) {
        return primaryMonitor();
    }

    public static String monitorName(int id) {
        return "Folium Browser Display";
    }
}
