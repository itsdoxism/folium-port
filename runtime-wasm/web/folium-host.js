const foliumHostState = {
    webGpuAvailable: false,
    deviceReady: false,
    adapterName: "",
    deviceToken: 0,
    adapter: null,
    device: null,
    context: null,
    format: null
};

globalThis.__foliumHostState = foliumHostState;

export async function prepareFoliumGraphics() {
    foliumHostState.webGpuAvailable =
        typeof navigator !== "undefined" && !!navigator.gpu;

    if (!foliumHostState.webGpuAvailable) {
        return foliumHostState;
    }

    const canvas = document.getElementById("folium-canvas");
    if (!canvas) {
        throw new Error("Folium canvas is missing");
    }

    const adapter = await navigator.gpu.requestAdapter({
        powerPreference: "high-performance"
    });

    if (!adapter) {
        throw new Error("No WebGPU adapter available");
    }

    const device = await adapter.requestDevice();
    const context = canvas.getContext("webgpu");

    if (!context) {
        throw new Error("Canvas WebGPU context unavailable");
    }

    const format = navigator.gpu.getPreferredCanvasFormat();

    context.configure({
        device,
        format,
        alphaMode: "opaque"
    });

    foliumHostState.adapter = adapter;
    foliumHostState.device = device;
    foliumHostState.context = context;
    foliumHostState.format = format;
    foliumHostState.deviceReady = true;
    foliumHostState.deviceToken = 1;

    // adapter.info is not uniformly exposed on every browser build.
    try {
        const info = adapter.info || {};
        foliumHostState.adapterName =
            info.description || info.device || info.vendor || "WebGPU adapter";
    } catch {
        foliumHostState.adapterName = "WebGPU adapter";
    }

    return foliumHostState;
}

export function renderFoliumBootstrapFrame() {
    if (!foliumHostState.deviceReady) {
        throw new Error("Folium WebGPU device has not been prepared");
    }

    const { device, context } = foliumHostState;

    const encoder = device.createCommandEncoder({
        label: "Folium bootstrap encoder"
    });

    const pass = encoder.beginRenderPass({
        label: "Folium bootstrap clear",
        colorAttachments: [{
            view: context.getCurrentTexture().createView(),
            clearValue: { r: 0.035, g: 0.055, b: 0.045, a: 1.0 },
            loadOp: "clear",
            storeOp: "store"
        }]
    });

    pass.end();
    device.queue.submit([encoder.finish()]);
}
