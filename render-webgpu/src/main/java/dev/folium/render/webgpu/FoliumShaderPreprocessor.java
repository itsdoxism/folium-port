package dev.folium.render.webgpu;

import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FoliumShaderPreprocessor {
    private static final Pattern INCLUDE = Pattern.compile(
        "(?m)^\\s*#include\\s+[<\"]([^>\"]+)[>\"]\\s*$"
    );

    private FoliumShaderPreprocessor() {
    }

    public static String prepare(
        Identifier shaderId,
        String source,
        ShaderDefines defines
    ) {
        String expanded = FoliumShaderIncludeExpander.expand(shaderId, source);
        return applyDefinesAndValidate(shaderId, expanded, defines);
    }

    public static String applyDefinesAndValidate(
        Identifier shaderId,
        String source,
        ShaderDefines defines
    ) {
        if (source == null) {
            throw new NullPointerException("source");
        }

        Matcher include = INCLUDE.matcher(source);
        if (include.find()) {
            throw new IllegalStateException(
                "Folium shader " + shaderId +
                    " still contains unresolved include: " +
                    include.group(1)
            );
        }

        if (defines == null || defines.isEmpty()) {
            return source;
        }

        String directives = defines.asSourceDirectives();
        int versionEnd = findVersionLineEnd(source);

        if (versionEnd < 0) {
            return directives + "\n" + source;
        }

        return source.substring(0, versionEnd) +
            directives +
            source.substring(versionEnd);
    }

    private static int findVersionLineEnd(String source) {
        int version = source.indexOf("#version");
        if (version < 0) {
            return -1;
        }

        int newline = source.indexOf('\n', version);
        return newline < 0 ? source.length() : newline + 1;
    }
}
