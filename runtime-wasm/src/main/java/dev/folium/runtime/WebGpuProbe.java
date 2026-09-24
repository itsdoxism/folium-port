package dev.folium.runtime;

import org.teavm.jso.JSBody;

public final class WebGpuProbe {
    private WebGpuProbe() {
    }

    @JSBody(script = "return !!globalThis.__foliumWebGpu?.device;")
    public static native boolean isReady();

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
        const state = globalThis.__foliumWebGpu;
        if (!state?.device || !state?.context) return false;

        const encoder = state.device.createCommandEncoder({
            label: 'Folium first frame'
        });

        const pass = encoder.beginRenderPass({
            label: 'Folium clear pass',
            colorAttachments: [{
                view: state.context.getCurrentTexture().createView(),
                clearValue: { r: 0.035, g: 0.055, b: 0.045, a: 1.0 },
                loadOp: 'clear',
                storeOp: 'store'
            }]
        });

        pass.end();
        state.device.queue.submit([encoder.finish()]);
        return true;
    """)
    public static native boolean renderFirstFrame();
}
