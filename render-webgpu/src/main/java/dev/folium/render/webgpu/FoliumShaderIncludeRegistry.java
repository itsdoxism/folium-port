package dev.folium.render.webgpu;

import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Browser-safe raw shader include storage.
 *
 * ShaderManager's normal include representation is backed by native shaderc.
 * Folium keeps the original text separately and swaps a complete snapshot at
 * the end of each resource reload.
 */
public final class FoliumShaderIncludeRegistry {
    private static Map<Identifier, String> active = Map.of();
    private static Map<Identifier, String> staging;

    private FoliumShaderIncludeRegistry() {
    }

    public static synchronized void beginReload() {
        staging = new LinkedHashMap<>();
    }

    public static synchronized void stage(Identifier id, String source) {
        if (staging == null) {
            throw new IllegalStateException(
                "Folium shader include reload has not been started"
            );
        }

        staging.put(id, source);
    }

    public static synchronized void commitReload() {
        if (staging == null) {
            throw new IllegalStateException(
                "Folium shader include reload has not been started"
            );
        }

        active = Map.copyOf(staging);
        staging = null;
    }

    public static synchronized void abortReload() {
        staging = null;
    }

    public static synchronized String get(Identifier id) {
        return active.get(id);
    }

    public static synchronized boolean contains(Identifier id) {
        return active.containsKey(id);
    }

    public static synchronized int size() {
        return active.size();
    }

    public static synchronized Map<Identifier, String> snapshot() {
        return active;
    }
}
