package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates Minecraft 26.3 RenderPearl vertex layout metadata into the subset
 * accepted by WebGPU.
 */
public final class FoliumVertexFormatMapper {
    private FoliumVertexFormatMapper() {
    }

    public static FoliumVertexLayout map(VertexFormat format) {
        if (format.getVertexSize() <= 0) {
            throw new IllegalArgumentException("Vertex stride must be positive");
        }

        FoliumVertexLayout.StepMode stepMode = switch (format.getStepRate()) {
            case 0 -> FoliumVertexLayout.StepMode.VERTEX;
            case 1 -> FoliumVertexLayout.StepMode.INSTANCE;
            default -> throw new UnsupportedOperationException(
                "Folium WebGPU does not yet support vertex stepRate=" +
                    format.getStepRate()
            );
        };

        List<FoliumVertexLayout.Attribute> attributes = new ArrayList<>();
        int shaderLocation = 0;

        for (VertexFormatElement element : format.getElements()) {
            validateElementBounds(format, element);

            attributes.add(new FoliumVertexLayout.Attribute(
                shaderLocation++,
                element.offset(),
                toWebGpuVertexFormat(element.format()),
                element.name()
            ));
        }

        return new FoliumVertexLayout(
            format.getVertexSize(),
            stepMode,
            List.copyOf(attributes)
        );
    }

    static String toWebGpuVertexFormat(GpuFormat format) {
        return switch (format) {
            case R8_UINT -> "uint8";
            case R8_SINT -> "sint8";
            case RG8_UINT -> "uint8x2";
            case RG8_SINT -> "sint8x2";
            case RGBA8_UINT -> "uint8x4";
            case RGBA8_SINT -> "sint8x4";

            case R8_UNORM -> "unorm8";
            case R8_SNORM -> "snorm8";
            case RG8_UNORM -> "unorm8x2";
            case RG8_SNORM -> "snorm8x2";
            case RGBA8_UNORM -> "unorm8x4";
            case RGBA8_SNORM -> "snorm8x4";

            case R16_UINT -> "uint16";
            case R16_SINT -> "sint16";
            case RG16_UINT -> "uint16x2";
            case RG16_SINT -> "sint16x2";
            case RGBA16_UINT -> "uint16x4";
            case RGBA16_SINT -> "sint16x4";

            case R16_UNORM -> "unorm16";
            case R16_SNORM -> "snorm16";
            case RG16_UNORM -> "unorm16x2";
            case RG16_SNORM -> "snorm16x2";
            case RGBA16_UNORM -> "unorm16x4";
            case RGBA16_SNORM -> "snorm16x4";

            case RG16_FLOAT -> "float16x2";
            case RGBA16_FLOAT -> "float16x4";

            case R32_UINT -> "uint32";
            case RG32_UINT -> "uint32x2";
            case RGB32_UINT -> "uint32x3";
            case RGBA32_UINT -> "uint32x4";

            case R32_SINT -> "sint32";
            case RG32_SINT -> "sint32x2";
            case RGB32_SINT -> "sint32x3";
            case RGBA32_SINT -> "sint32x4";

            case R32_FLOAT -> "float32";
            case RG32_FLOAT -> "float32x2";
            case RGB32_FLOAT -> "float32x3";
            case RGBA32_FLOAT -> "float32x4";

            default -> throw new UnsupportedOperationException(
                "Minecraft vertex format " + format +
                    " has no direct WebGPU vertex attribute representation"
            );
        };
    }

    private static void validateElementBounds(
        VertexFormat format,
        VertexFormatElement element
    ) {
        int end = element.offset() + element.format().blockSize();

        if (element.offset() < 0 || end > format.getVertexSize()) {
            throw new IllegalArgumentException(
                "Vertex element " + element.name() +
                    " exceeds stride " + format.getVertexSize()
            );
        }
    }
}
