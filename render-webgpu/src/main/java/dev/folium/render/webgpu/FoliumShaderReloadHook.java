package dev.folium.render.webgpu;

import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;

/**
 * Tiny patch target for ShaderManager's resource reload path.
 *
 * The eventual Folium bytecode/source patch only needs to call these methods;
 * the registry and lifecycle remain outside Minecraft-owned code.
 */
public final class FoliumShaderReloadHook {
    private FoliumShaderReloadHook() {
    }

    public static void beginReload() {
        FoliumShaderIncludeRegistry.beginReload();
    }

    public static void captureInclude(
        Identifier resourceLocation,
        Resource resource
    ) throws IOException {
        captureInclude(
            includeIdFromResource(resourceLocation),
            resource.readAllAsString()
        );
    }

    public static void captureInclude(
        Identifier includeId,
        String rawSource
    ) {
        FoliumShaderIncludeRegistry.stage(includeId, rawSource);
    }

    public static void commitReload() {
        FoliumShaderIncludeRegistry.commitReload();
    }

    public static void abortReload() {
        FoliumShaderIncludeRegistry.abortReload();
    }

    public static Identifier includeIdFromResource(
        Identifier resourceLocation
    ) {
        return ShaderManager.SHADER_INCLUDE_CONVERTER
            .fileToId(resourceLocation)
            .withSuffix(".glsl");
    }
}
