package dev.folium.runtime;

import org.teavm.jso.JSBody;

public final class WebGpuProbe {
    private WebGpuProbe() {
    }

    @JSBody(script = "return typeof navigator !== 'undefined' && !!navigator.gpu;")
    public static native boolean isAvailable();

    @JSBody(
        params = {"message", "ok"},
        script = """
            const node = document.getElementById('status');
            if (node) {
                node.textContent = message;
                node.dataset.ok = ok ? 'true' : 'false';
            }
        """
    )
    public static native void setStatus(String message, boolean ok);

    @JSBody(script = """
        const canvas = document.getElementById('folium-canvas');
        if (!canvas) return false;
        canvas.width = Math.max(1, Math.floor(canvas.clientWidth * devicePixelRatio));
        canvas.height = Math.max(1, Math.floor(canvas.clientHeight * devicePixelRatio));
        return true;
    """)
    public static native boolean prepareCanvas();
}
