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

const resources = new Map();
let nextToken = 1;

function storeResource(value) {
    const token = nextToken++;
    resources.set(token, value);
    return token;
}

function requireResource(token, kind) {
    const value = resources.get(token);
    if (!value || value.kind !== kind) {
        throw new Error(`Invalid Folium ${kind} token: ${token}`);
    }
    return value.value;
}

function toWebGpuFormat(format) {
    switch (format) {
        case "R8_UNORM": return "r8unorm";
        case "RG8_UNORM": return "rg8unorm";
        case "RGBA8_UNORM": return "rgba8unorm";
        case "RGBA8_SNORM": return "rgba8snorm";
        case "R16_FLOAT": return "r16float";
        case "RG16_FLOAT": return "rg16float";
        case "RGBA16_FLOAT": return "rgba16float";
        case "R32_FLOAT": return "r32float";
        case "RG32_FLOAT": return "rg32float";
        case "RGBA32_FLOAT": return "rgba32float";
        case "D16_UNORM": return "depth16unorm";
        case "D32_FLOAT": return "depth32float";
        case "D24_UNORM_S8_UINT": return "depth24plus-stencil8";
        case "D32_FLOAT_S8_UINT": return "depth32float-stencil8";
        default:
            throw new Error(`Unsupported Folium WebGPU texture format: ${format}`);
    }
}

function toWebGpuTextureUsage(renderPearlUsage) {
    /*
     * RenderPearl's usage bit values are intentionally not hard-coded here
     * yet. The first port stage allocates a safe superset for resources that
     * reach this bridge. Tight usage translation can follow once all constants
     * are mapped from 26.3.
     */
    return GPUTextureUsage.COPY_SRC |
        GPUTextureUsage.COPY_DST |
        GPUTextureUsage.TEXTURE_BINDING |
        GPUTextureUsage.RENDER_ATTACHMENT;
}

globalThis.__foliumHostState = foliumHostState;

globalThis.__foliumWebGpuBridge = {
    createTexture(label, usage, format, width, height, depthOrLayers, mipLevels) {
        if (!foliumHostState.deviceReady) {
            throw new Error("Folium WebGPU device is not ready");
        }

        const texture = foliumHostState.device.createTexture({
            label: label || "Folium texture",
            size: {
                width,
                height,
                depthOrArrayLayers: depthOrLayers
            },
            mipLevelCount: mipLevels,
            format: toWebGpuFormat(format),
            usage: toWebGpuTextureUsage(usage)
        });

        return storeResource({ kind: "texture", value: texture });
    },

    destroyTexture(textureToken) {
        const texture = requireResource(textureToken, "texture");
        texture.destroy();
        resources.delete(textureToken);
    },

    createCommandEncoder() {
        if (!foliumHostState.deviceReady) {
            throw new Error("Folium WebGPU device is not ready");
        }

        const encoder = foliumHostState.device.createCommandEncoder({
            label: "Folium RenderPearl encoder"
        });

        return storeResource({ kind: "encoder", value: encoder });
    },

    clearColorTexture(encoderToken, textureToken, red, green, blue, alpha) {
        const encoder = requireResource(encoderToken, "encoder");
        const texture = requireResource(textureToken, "texture");

        const pass = encoder.beginRenderPass({
            label: "Folium clearColorTexture",
            colorAttachments: [{
                view: texture.createView(),
                clearValue: { r: red, g: green, b: blue, a: alpha },
                loadOp: "clear",
                storeOp: "store"
            }]
        });

        pass.end();
    },

    submitCommandEncoder(encoderToken) {
        const encoder = requireResource(encoderToken, "encoder");
        foliumHostState.device.queue.submit([encoder.finish()]);
        resources.delete(encoderToken);
    }
};

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
