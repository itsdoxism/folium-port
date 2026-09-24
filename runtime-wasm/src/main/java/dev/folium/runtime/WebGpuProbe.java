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

    /**
     * First real browser graphics probe.
     *
     * The async WebGPU setup intentionally lives on the host side for now.
     * Once this path is proven, Folium will move resource and command concepts
     * behind Java wrappers that implement RenderPearl's interfaces.
     */
    @JSBody(script = """
        const status = document.getElementById('status');
        const canvas = document.getElementById('folium-canvas');

        const fail = (message, error) => {
            if (error) console.error(error);
            if (status) {
                status.textContent = message;
                status.dataset.ok = 'false';
            }
        };

        (async () => {
            if (!navigator.gpu) {
                fail('Folium WASM bridge OK · WebGPU unavailable');
                return;
            }

            if (!canvas) {
                fail('Folium WASM bridge OK · canvas missing');
                return;
            }

            try {
                const adapter = await navigator.gpu.requestAdapter({
                    powerPreference: 'high-performance'
                });

                if (!adapter) {
                    fail('Folium WebGPU: no adapter');
                    return;
                }

                const device = await adapter.requestDevice();
                const context = canvas.getContext('webgpu');

                if (!context) {
                    fail('Folium WebGPU: canvas webgpu context unavailable');
                    return;
                }

                const format = navigator.gpu.getPreferredCanvasFormat();
                context.configure({
                    device,
                    format,
                    alphaMode: 'opaque'
                });

                const encoder = device.createCommandEncoder({
                    label: 'Folium first frame'
                });

                const pass = encoder.beginRenderPass({
                    label: 'Folium clear pass',
                    colorAttachments: [{
                        view: context.getCurrentTexture().createView(),
                        clearValue: { r: 0.035, g: 0.055, b: 0.045, a: 1.0 },
                        loadOp: 'clear',
                        storeOp: 'store'
                    }]
                });

                pass.end();
                device.queue.submit([encoder.finish()]);

                globalThis.__foliumWebGpu = {
                    adapter,
                    device,
                    context,
                    format
                };

                if (status) {
                    status.textContent = 'Folium WASM-GC → WebGPU first frame OK';
                    status.dataset.ok = 'true';
                }
            } catch (error) {
                fail('Folium WebGPU initialization failed — see console', error);
            }
        })();
    """)
    public static native void renderFirstFrame();
}
