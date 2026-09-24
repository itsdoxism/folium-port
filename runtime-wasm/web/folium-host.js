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

function storeResource(kind, value) {
    const token = nextToken++;
    resources.set(token, { kind, value });
    return token;
}

function requireResource(token, kind) {
    const entry = resources.get(token);
    if (!entry || entry.kind !== kind) {
        throw new Error(`Invalid Folium ${kind} token: ${token}`);
    }
    return entry.value;
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

        return storeResource("texture", texture);
    },

    destroyTexture(textureToken) {
        const texture = requireResource(textureToken, "texture");
        texture.destroy();
        resources.delete(textureToken);
    },

    createTextureView(textureToken, baseMipLevel, mipLevels) {
        const texture = requireResource(textureToken, "texture");
        const view = texture.createView({
            baseMipLevel,
            mipLevelCount: mipLevels
        });
        return storeResource("textureView", view);
    },

    releaseTextureView(textureViewToken) {
        requireResource(textureViewToken, "textureView");
        resources.delete(textureViewToken);
    },

    createCommandEncoder() {
        if (!foliumHostState.deviceReady) {
            throw new Error("Folium WebGPU device is not ready");
        }

        const encoder = foliumHostState.device.createCommandEncoder({
            label: "Folium RenderPearl encoder"
        });

        return storeResource("encoder", encoder);
    },

    beginColorRenderPass(
        encoderToken,
        textureViewToken,
        clear,
        red,
        green,
        blue,
        alpha
    ) {
        const encoder = requireResource(encoderToken, "encoder");
        const view = requireResource(textureViewToken, "textureView");

        const attachment = {
            view,
            loadOp: clear ? "clear" : "load",
            storeOp: "store"
        };

        if (clear) {
            attachment.clearValue = { r: red, g: green, b: blue, a: alpha };
        }

        const pass = encoder.beginRenderPass({
            label: "Folium RenderPearl render pass",
            colorAttachments: [attachment]
        });

        return storeResource("renderPass", pass);
    },

    pushRenderPassDebugGroup(renderPassToken, label) {
        const pass = requireResource(renderPassToken, "renderPass");
        pass.pushDebugGroup(label || "Folium");
    },

    popRenderPassDebugGroup(renderPassToken) {
        const pass = requireResource(renderPassToken, "renderPass");
        pass.popDebugGroup();
    },

    setRenderPassScissor(renderPassToken, x, y, width, height) {
        const pass = requireResource(renderPassToken, "renderPass");
        pass.setScissorRect(x, y, width, height);
    },

    endRenderPass(renderPassToken) {
        const pass = requireResource(renderPassToken, "renderPass");
        pass.end();
        resources.delete(renderPassToken);
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
