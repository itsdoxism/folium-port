package dev.folium.render.webgpu;

public final class FoliumVertexLayoutJson {
    private FoliumVertexLayoutJson() {
    }

    public static String encode(FoliumVertexLayout layout) {
        StringBuilder out = new StringBuilder();
        out.append('{');
        out.append("\"arrayStride\":").append(layout.arrayStride()).append(',');
        out.append("\"stepMode\":\"")
            .append(layout.stepMode() == FoliumVertexLayout.StepMode.INSTANCE ? "instance" : "vertex")
            .append("\",");
        out.append("\"attributes\":[");

        for (int i = 0; i < layout.attributes().size(); i++) {
            if (i > 0) {
                out.append(',');
            }

            FoliumVertexLayout.Attribute attribute = layout.attributes().get(i);
            out.append('{')
                .append("\"shaderLocation\":").append(attribute.shaderLocation()).append(',')
                .append("\"offset\":").append(attribute.offset()).append(',')
                .append("\"webGpuFormat\":\"").append(attribute.webGpuFormat()).append("\"")
                .append('}');
        }

        out.append("]}");
        return out.toString();
    }
}
