package dev.folium.render.webgpu;

import net.minecraft.resources.Identifier;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FoliumShaderIncludeExpander {
    private static final int MAX_INCLUDE_DEPTH = 64;

    private static final Pattern INCLUDE = Pattern.compile(
        "(?m)^(\\s*)#include\\s+[<\"]([^>\"]+)[>\"]\\s*$"
    );

    private FoliumShaderIncludeExpander() {
    }

    public static String expand(Identifier shaderId, String source) {
        return expandSource(shaderId, source, new ArrayDeque<>(), 0);
    }

    private static String expandSource(
        Identifier owner,
        String source,
        Deque<Identifier> stack,
        int depth
    ) {
        if (depth > MAX_INCLUDE_DEPTH) {
            throw new IllegalStateException(
                "Folium shader include depth exceeded " + MAX_INCLUDE_DEPTH +
                    " while expanding " + owner
            );
        }

        Matcher matcher = INCLUDE.matcher(source);
        StringBuffer expanded = new StringBuffer();

        while (matcher.find()) {
            Identifier includeId = parseIncludeId(matcher.group(2));

            if (stack.contains(includeId)) {
                throw new IllegalStateException(
                    "Folium shader include cycle: " +
                        describeCycle(stack, includeId)
                );
            }

            String includeSource = FoliumShaderIncludeRegistry.get(includeId);
            if (includeSource == null) {
                throw new IllegalStateException(
                    "Missing Folium raw shader include " + includeId +
                        " required by " + owner
                );
            }

            stack.addLast(includeId);
            String nested = expandSource(
                includeId,
                includeSource,
                stack,
                depth + 1
            );
            stack.removeLast();

            String replacement =
                "\n// Folium include begin: " + includeId + "\n" +
                nested +
                "\n// Folium include end: " + includeId + "\n";

            matcher.appendReplacement(
                expanded,
                Matcher.quoteReplacement(replacement)
            );
        }

        matcher.appendTail(expanded);
        return expanded.toString();
    }

    private static Identifier parseIncludeId(String value) {
        if (value.indexOf(':') >= 0) {
            return Identifier.parse(value);
        }

        return Identifier.withDefaultNamespace(value);
    }

    private static String describeCycle(
        Deque<Identifier> stack,
        Identifier repeated
    ) {
        StringBuilder out = new StringBuilder();

        for (Identifier id : stack) {
            if (!out.isEmpty()) {
                out.append(" -> ");
            }
            out.append(id);
        }

        if (!out.isEmpty()) {
            out.append(" -> ");
        }
        out.append(repeated);

        return out.toString();
    }
}
