package dev.folium.render.webgpu;

public final class FoliumPipelineStateJson {
    private FoliumPipelineStateJson() {
    }

    public static String encode(FoliumPipelineState state) {
        StringBuilder out = new StringBuilder();
        out.append('{');
        field(out, "primitiveTopology", state.primitiveTopology()).append(',');
        field(out, "frontFace", state.frontFace()).append(',');
        field(out, "cullMode", state.cullMode()).append(',');
        field(out, "polygonMode", state.polygonMode()).append(',');

        out.append("\"vertexBuffers\":[");
        for (int i = 0; i < state.vertexBuffers().size(); i++) {
            if (i > 0) out.append(',');
            out.append(FoliumVertexLayoutJson.encode(state.vertexBuffers().get(i)));
        }
        out.append("],");

        out.append("\"colorTargets\":[");
        for (int i = 0; i < state.colorTargets().size(); i++) {
            if (i > 0) out.append(',');
            appendColorTarget(out, state.colorTargets().get(i));
        }
        out.append("],");

        out.append("\"depthState\":");
        if (state.depthState() == null) {
            out.append("null");
        } else {
            appendDepthState(out, state.depthState());
        }

        out.append('}');
        return out.toString();
    }

    private static void appendColorTarget(
        StringBuilder out,
        FoliumPipelineState.ColorTarget target
    ) {
        out.append('{');
        field(out, "format", target.format()).append(',');
        out.append("\"writeMask\":").append(target.writeMask()).append(',');
        out.append("\"blend\":");

        if (target.blend() == null) {
            out.append("null");
        } else {
            out.append('{');
            out.append("\"color\":");
            appendBlendComponent(out, target.blend().color());
            out.append(',');
            out.append("\"alpha\":");
            appendBlendComponent(out, target.blend().alpha());
            out.append('}');
        }

        out.append('}');
    }

    private static void appendBlendComponent(
        StringBuilder out,
        FoliumPipelineState.BlendComponent component
    ) {
        out.append('{');
        field(out, "operation", component.operation()).append(',');
        field(out, "srcFactor", component.srcFactor()).append(',');
        field(out, "dstFactor", component.dstFactor());
        out.append('}');
    }

    private static void appendDepthState(
        StringBuilder out,
        FoliumPipelineState.DepthState depth
    ) {
        out.append('{');
        field(out, "format", depth.format()).append(',');
        out.append("\"depthWriteEnabled\":")
            .append(depth.depthWriteEnabled()).append(',');
        field(out, "depthCompare", depth.depthCompare()).append(',');
        out.append("\"depthBiasSlopeScale\":")
            .append(depth.depthBiasSlopeScale()).append(',');
        out.append("\"depthBias\":").append(depth.depthBias());
        out.append('}');
    }

    private static StringBuilder field(
        StringBuilder out,
        String name,
        String value
    ) {
        return out.append('\"').append(name).append("\":\"")
            .append(value).append('\"');
    }
}
