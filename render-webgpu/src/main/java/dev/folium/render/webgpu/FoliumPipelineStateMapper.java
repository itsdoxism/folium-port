package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BlendEquation;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.BlendOp;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PolygonMode;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import java.util.ArrayList;
import java.util.List;

public final class FoliumPipelineStateMapper {
    private FoliumPipelineStateMapper() {
    }

    public static FoliumPipelineState map(RenderPipeline pipeline) {
        String polygonMode = mapPolygonMode(pipeline.getPolygonMode());
        String topology = mapTopology(pipeline.getPrimitiveTopology());

        List<FoliumVertexLayout> vertexBuffers = pipeline.getVertexFormatBindings()
            .stream()
            .map(FoliumVertexFormatMapper::map)
            .toList();

        List<FoliumPipelineState.ColorTarget> colorTargets = new ArrayList<>();
        for (ColorTargetState target : pipeline.getColorTargetStates()) {
            colorTargets.add(mapColorTarget(target));
        }

        FoliumPipelineState.DepthState depthState =
            mapDepthState(pipeline.getDepthStencilState());

        return new FoliumPipelineState(
            topology,
            "ccw",
            pipeline.isCull() ? "back" : "none",
            polygonMode,
            vertexBuffers,
            List.copyOf(colorTargets),
            depthState
        );
    }

    private static String mapTopology(PrimitiveTopology topology) {
        return switch (topology) {
            case POINTS -> "point-list";
            case LINES, DEBUG_LINES -> "line-list";
            case DEBUG_LINE_STRIP -> "line-strip";
            case TRIANGLES -> "triangle-list";
            case TRIANGLE_STRIP -> "triangle-strip";
            case TRIANGLE_FAN, QUADS -> throw new UnsupportedOperationException(
                "Folium WebGPU requires topology expansion for " + topology
            );
        };
    }

    private static String mapPolygonMode(PolygonMode mode) {
        return switch (mode) {
            case FILL -> "fill";
            case WIREFRAME -> throw new UnsupportedOperationException(
                "WebGPU has no core wireframe polygon mode"
            );
        };
    }

    private static FoliumPipelineState.ColorTarget mapColorTarget(
        ColorTargetState target
    ) {
        FoliumPipelineState.BlendState blend = target.blendFunction()
            .map(FoliumPipelineStateMapper::mapBlend)
            .orElse(null);

        return new FoliumPipelineState.ColorTarget(
            mapColorFormat(target.format()),
            mapWriteMask(target),
            blend
        );
    }

    private static FoliumPipelineState.BlendState mapBlend(BlendFunction blend) {
        return new FoliumPipelineState.BlendState(
            mapBlendEquation(blend.color()),
            mapBlendEquation(blend.alpha())
        );
    }

    private static FoliumPipelineState.BlendComponent mapBlendEquation(
        BlendEquation equation
    ) {
        return new FoliumPipelineState.BlendComponent(
            mapBlendOp(equation.op()),
            mapBlendFactor(equation.sourceFactor()),
            mapBlendFactor(equation.destFactor())
        );
    }

    private static String mapBlendOp(BlendOp op) {
        return switch (op) {
            case ADD -> "add";
            case SUBTRACT -> "subtract";
            case REVERSE_SUBTRACT -> "reverse-subtract";
            case MIN -> "min";
            case MAX -> "max";
        };
    }

    private static String mapBlendFactor(BlendFactor factor) {
        return switch (factor) {
            case ZERO -> "zero";
            case ONE -> "one";
            case SRC_COLOR -> "src";
            case ONE_MINUS_SRC_COLOR -> "one-minus-src";
            case SRC_ALPHA -> "src-alpha";
            case ONE_MINUS_SRC_ALPHA -> "one-minus-src-alpha";
            case DST_COLOR -> "dst";
            case ONE_MINUS_DST_COLOR -> "one-minus-dst";
            case DST_ALPHA -> "dst-alpha";
            case ONE_MINUS_DST_ALPHA -> "one-minus-dst-alpha";
            case SRC_ALPHA_SATURATE -> "src-alpha-saturated";
            case CONSTANT_COLOR, CONSTANT_ALPHA -> "constant";
            case ONE_MINUS_CONSTANT_COLOR, ONE_MINUS_CONSTANT_ALPHA ->
                "one-minus-constant";
        };
    }

    private static FoliumPipelineState.DepthState mapDepthState(
        DepthStencilState state
    ) {
        if (state == null) {
            return null;
        }

        return new FoliumPipelineState.DepthState(
            "depth24plus",
            state.writeDepth(),
            mapCompare(state.depthTest()),
            state.depthBiasScaleFactor(),
            Math.round(state.depthBiasConstant())
        );
    }

    private static String mapCompare(CompareOp op) {
        return switch (op) {
            case NEVER -> "never";
            case LESS -> "less";
            case EQUAL -> "equal";
            case LESS_OR_EQUAL -> "less-equal";
            case GREATER -> "greater";
            case NOT_EQUAL -> "not-equal";
            case GREATER_OR_EQUAL -> "greater-equal";
            case ALWAYS -> "always";
        };
    }

    private static int mapWriteMask(ColorTargetState target) {
        int mask = 0;
        if (target.writeRed()) mask |= 1;
        if (target.writeGreen()) mask |= 2;
        if (target.writeBlue()) mask |= 4;
        if (target.writeAlpha()) mask |= 8;
        return mask;
    }

    private static String mapColorFormat(GpuFormat format) {
        return switch (format) {
            case RGBA8_UNORM -> "rgba8unorm";
            case RGBA8_SRGB -> "rgba8unorm-srgb";
            case BGRA8_UNORM -> "bgra8unorm";
            case BGRA8_SRGB -> "bgra8unorm-srgb";
            case RGBA16_FLOAT -> "rgba16float";
            case RGBA32_FLOAT -> "rgba32float";
            default -> throw new UnsupportedOperationException(
                "Unsupported WebGPU color target format: " + format
            );
        };
    }
}
