const status = document.getElementById("status");
const canvas = document.getElementById("folium-canvas");

function setStatus(message, ok = false) {
    if (!status) return;
    status.textContent = message;
    status.dataset.ok = ok ? "true" : "false";
}

function resizeCanvas() {
    if (!canvas) {
        throw new Error("Folium canvas is missing");
    }

    canvas.width = Math.max(1, Math.floor(canvas.clientWidth * devicePixelRatio));
    canvas.height = Math.max(1, Math.floor(canvas.clientHeight * devicePixelRatio));
}

async function initializeWebGpu() {
    if (!navigator.gpu) {
        throw new Error("WebGPU is unavailable");
    }

    setStatus("Folium · requesting WebGPU adapter…");

    const adapter = await navigator.gpu.requestAdapter({
        powerPreference: "high-performance"
    });

    if (!adapter) {
        throw new Error("No WebGPU adapter was returned");
    }

    setStatus("Folium · requesting WebGPU device…");

    const device = await adapter.requestDevice();
    const context = canvas.getContext("webgpu");

    if (!context) {
        throw new Error("Could not acquire a WebGPU canvas context");
    }

    resizeCanvas();

    const format = navigator.gpu.getPreferredCanvasFormat();
    context.configure({
        device,
        format,
        alphaMode: "opaque"
    });

    globalThis.__foliumWebGpu = {
        adapter,
        device,
        context,
        format,
        canvas
    };

    return globalThis.__foliumWebGpu;
}

async function loadFoliumWasm() {
    setStatus("Folium · loading WASM-GC runtime…");

    const module = await TeaVM.wasmGC.load(
        "../build/generated/teavm/wasm-gc/folium-smoke.wasm"
    );

    module.exports.main([]);
}

async function boot() {
    try {
        await initializeWebGpu();
        await loadFoliumWasm();
    } catch (error) {
        console.error(error);
        setStatus("Folium boot failed — see console", false);
    }
}

boot();
