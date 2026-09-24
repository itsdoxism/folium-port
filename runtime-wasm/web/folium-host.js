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

function normalizePipelineFormat(format) {
    if (!format) return foliumHostState.format;
    if (format.includes("-")) return format;
    return toWebGpuFormat(format);
}

function toWebGpuTextureUsage(renderPearlUsage) {
    return GPUTextureUsage.COPY_SRC |
        GPUTextureUsage.COPY_DST |
        GPUTextureUsage.TEXTURE_BINDING |
        GPUTextureUsage.RENDER_ATTACHMENT;
}

function toWebGpuBufferUsage(renderPearlUsage) {
    let usage = 0;

    if (renderPearlUsage & 1) usage |= GPUBufferUsage.MAP_READ;
    if (renderPearlUsage & 2) usage |= GPUBufferUsage.MAP_WRITE;
    if (renderPearlUsage & 8) usage |= GPUBufferUsage.COPY_DST;
    if (renderPearlUsage & 16) usage |= GPUBufferUsage.COPY_SRC;
    if (renderPearlUsage & 32) usage |= GPUBufferUsage.VERTEX;
    if (renderPearlUsage & 64) usage |= GPUBufferUsage.INDEX;
    if (renderPearlUsage & 128) usage |= GPUBufferUsage.UNIFORM;
    if (renderPearlUsage & 512) usage |= GPUBufferUsage.INDIRECT;

    if (usage === 0) usage = GPUBufferUsage.COPY_DST;
    return usage;
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

    createBuffer(label, usage, size) {
        if (!foliumHostState.deviceReady) {
            throw new Error("Folium WebGPU device is not ready");
        }

        const numericSize = Number(size);
        if (!Number.isSafeInteger(numericSize) || numericSize <= 0) {
            throw new Error(`Invalid Folium buffer size: ${size}`);
        }

        const alignedSize = (numericSize + 3) & ~3;
        const buffer = foliumHostState.device.createBuffer({
            label: label || "Folium buffer",
            size: alignedSize,
            usage: toWebGpuBufferUsage(usage)
        });

        return storeResource("buffer", buffer);
    },

    writeBuffer(bufferToken, offset, data) {
        const buffer = requireResource(bufferToken, "buffer");
        if (!data || typeof data.byteLength !== "number") {
            throw new Error("Folium writeBuffer expected an ArrayBufferView");
        }

        const destinationOffset = Number(offset);
        if (destinationOffset % 4 !== 0) {
            throw new Error("Folium WebGPU buffer writes require 4-byte aligned offsets");
        }

        if (data.byteLength % 4 === 0) {
            foliumHostState.device.queue.writeBuffer(
                buffer,
                destinationOffset,
                data.buffer,
                data.byteOffset,
                data.byteLength
            );
            return;
        }

        // WebGPU writeBuffer requires a 4-byte-sized write. Pad only the rare
        // unaligned tail while preserving the Java buffer bytes exactly.
        const paddedLength = (data.byteLength + 3) & ~3;
        const padded = new Uint8Array(paddedLength);
        padded.set(new Uint8Array(data.buffer, data.byteOffset, data.byteLength));

        foliumHostState.device.queue.writeBuffer(
            buffer,
            destinationOffset,
            padded
        );
    },

    destroyBuffer(bufferToken) {
        const buffer = requireResource(bufferToken, "buffer");
        buffer.destroy();
        resources.delete(bufferToken);
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
        requireResource(renderPassToken, "renderPass")
            .pushDebugGroup(label || "Folium");
    },

    popRenderPassDebugGroup(renderPassToken) {
        requireResource(renderPassToken, "renderPass").popDebugGroup();
    },

    setRenderPassScissor(renderPassToken, x, y, width, height) {
        requireResource(renderPassToken, "renderPass")
            .setScissorRect(x, y, width, height);
    },

    createBootstrapTrianglePipeline(colorFormat) {
        if (!foliumHostState.deviceReady) {
            throw new Error("Folium WebGPU device is not ready");
        }

        const shader = foliumHostState.device.createShaderModule({
            label: "Folium bootstrap triangle shader",
            code: `
                struct VertexOut {
                    @builtin(position) position: vec4f,
                    @location(0) color: vec3f,
                };

                @vertex
                fn vs_main(@builtin(vertex_index) index: u32) -> VertexOut {
                    var positions = array<vec2f, 3>(
                        vec2f( 0.0,  0.62),
                        vec2f(-0.58, -0.46),
                        vec2f( 0.58, -0.46)
                    );

                    var colors = array<vec3f, 3>(
                        vec3f(0.45, 0.95, 0.66),
                        vec3f(0.25, 0.58, 0.98),
                        vec3f(0.92, 0.42, 0.64)
                    );

                    var out: VertexOut;
                    out.position = vec4f(positions[index], 0.0, 1.0);
                    out.color = colors[index];
                    return out;
                }

                @fragment
                fn fs_main(in: VertexOut) -> @location(0) vec4f {
                    return vec4f(in.color, 1.0);
                }
            `
        });

        const pipeline = foliumHostState.device.createRenderPipeline({
            label: "Folium bootstrap triangle pipeline",
            layout: "auto",
            vertex: {
                module: shader,
                entryPoint: "vs_main"
            },
            fragment: {
                module: shader,
                entryPoint: "fs_main",
                targets: [{
                    format: normalizePipelineFormat(colorFormat)
                }]
            },
            primitive: {
                topology: "triangle-list",
                cullMode: "none"
            }
        });

        return storeResource("pipeline", pipeline);
    },

    createVertexColorPipeline(layoutJson, colorFormat) {
        if (!foliumHostState.deviceReady) {
            throw new Error("Folium WebGPU device is not ready");
        }

        const layout = JSON.parse(layoutJson);
        const shader = foliumHostState.device.createShaderModule({
            label: "Folium vertex-buffer probe shader",
            code: `
                struct VertexIn {
                    @location(0) position: vec3f,
                    @location(1) color: vec4f,
                };

                struct VertexOut {
                    @builtin(position) position: vec4f,
                    @location(0) color: vec4f,
                };

                @vertex
                fn vs_main(input: VertexIn) -> VertexOut {
                    var out: VertexOut;
                    out.position = vec4f(input.position, 1.0);
                    out.color = input.color;
                    return out;
                }

                @fragment
                fn fs_main(input: VertexOut) -> @location(0) vec4f {
                    return input.color;
                }
            `
        });

        const pipeline = foliumHostState.device.createRenderPipeline({
            label: "Folium translated vertex-layout pipeline",
            layout: "auto",
            vertex: {
                module: shader,
                entryPoint: "vs_main",
                buffers: [{
                    arrayStride: layout.arrayStride,
                    stepMode: layout.stepMode,
                    attributes: layout.attributes.map(attribute => ({
                        shaderLocation: attribute.shaderLocation,
                        offset: attribute.offset,
                        format: attribute.webGpuFormat
                    }))
                }]
            },
            fragment: {
                module: shader,
                entryPoint: "fs_main",
                targets: [{
                    format: normalizePipelineFormat(colorFormat)
                }]
            },
            primitive: {
                topology: "triangle-list",
                cullMode: "none"
            }
        });

        return storeResource("pipeline", pipeline);
    },

    createPipelineFromState(pipelineStateJson) {
        if (!foliumHostState.deviceReady) {
            throw new Error("Folium WebGPU device is not ready");
        }

        const state = JSON.parse(pipelineStateJson);
        if (state.polygonMode !== "fill") {
            throw new Error(`Unsupported WebGPU polygon mode: ${state.polygonMode}`);
        }

        const shader = foliumHostState.device.createShaderModule({
            label: "Folium RenderPipeline state probe shader",
            code: `
                struct VertexIn {
                    @location(0) position: vec3f,
                    @location(1) color: vec4f,
                };

                struct VertexOut {
                    @builtin(position) position: vec4f,
                    @location(0) color: vec4f,
                };

                @vertex
                fn vs_main(input: VertexIn) -> VertexOut {
                    var out: VertexOut;
                    out.position = vec4f(input.position, 1.0);
                    out.color = input.color;
                    return out;
                }

                @fragment
                fn fs_main(input: VertexOut) -> @location(0) vec4f {
                    return input.color;
                }
            `
        });

        const buffers = state.vertexBuffers.map(layout => ({
            arrayStride: layout.arrayStride,
            stepMode: layout.stepMode,
            attributes: layout.attributes.map(attribute => ({
                shaderLocation: attribute.shaderLocation,
                offset: attribute.offset,
                format: attribute.webGpuFormat
            }))
        }));

        const targets = state.colorTargets.map(target => {
            const result = {
                format: target.format,
                writeMask: target.writeMask
            };

            if (target.blend) {
                result.blend = {
                    color: {
                        operation: target.blend.color.operation,
                        srcFactor: target.blend.color.srcFactor,
                        dstFactor: target.blend.color.dstFactor
                    },
                    alpha: {
                        operation: target.blend.alpha.operation,
                        srcFactor: target.blend.alpha.srcFactor,
                        dstFactor: target.blend.alpha.dstFactor
                    }
                };
            }

            return result;
        });

        const descriptor = {
            label: "Folium translated RenderPipeline",
            layout: "auto",
            vertex: {
                module: shader,
                entryPoint: "vs_main",
                buffers
            },
            fragment: {
                module: shader,
                entryPoint: "fs_main",
                targets
            },
            primitive: {
                topology: state.primitiveTopology,
                frontFace: state.frontFace,
                cullMode: state.cullMode
            }
        };

        if (state.depthState) {
            descriptor.depthStencil = {
                format: state.depthState.format,
                depthWriteEnabled: state.depthState.depthWriteEnabled,
                depthCompare: state.depthState.depthCompare,
                depthBias: state.depthState.depthBias,
                depthBiasSlopeScale: state.depthState.depthBiasSlopeScale
            };
        }

        const pipeline = foliumHostState.device.createRenderPipeline(descriptor);
        return storeResource("pipeline", pipeline);
    },

    createPipelineFromGlsl(pipelineStateJson, vertexGlsl, fragmentGlsl) {
        if (!foliumHostState.deviceReady) {
            throw new Error("Folium WebGPU device is not ready");
        }

        const translator = globalThis.__foliumShaderTranslator;
        if (!translator?.ready) {
            throw new Error("Folium shader translator is not ready");
        }

        const state = JSON.parse(pipelineStateJson);
        if (state.polygonMode !== "fill") {
            throw new Error(`Unsupported WebGPU polygon mode: ${state.polygonMode}`);
        }

        const vertexWgsl = translator.glslToWgsl(vertexGlsl, "vertex");
        const fragmentWgsl = translator.glslToWgsl(fragmentGlsl, "fragment");

        const vertexModule = foliumHostState.device.createShaderModule({
            label: "Folium Minecraft vertex shader",
            code: vertexWgsl
        });

        const fragmentModule = foliumHostState.device.createShaderModule({
            label: "Folium Minecraft fragment shader",
            code: fragmentWgsl
        });

        const buffers = state.vertexBuffers.map(layout => ({
            arrayStride: layout.arrayStride,
            stepMode: layout.stepMode,
            attributes: layout.attributes.map(attribute => ({
                shaderLocation: attribute.shaderLocation,
                offset: attribute.offset,
                format: attribute.webGpuFormat
            }))
        }));

        const targets = state.colorTargets.map(target => {
            const result = {
                format: target.format,
                writeMask: target.writeMask
            };

            if (target.blend) {
                result.blend = {
                    color: {
                        operation: target.blend.color.operation,
                        srcFactor: target.blend.color.srcFactor,
                        dstFactor: target.blend.color.dstFactor
                    },
                    alpha: {
                        operation: target.blend.alpha.operation,
                        srcFactor: target.blend.alpha.srcFactor,
                        dstFactor: target.blend.alpha.dstFactor
                    }
                };
            }

            return result;
        });

        const descriptor = {
            label: "Folium Minecraft RenderPipeline",
            layout: "auto",
            vertex: {
                module: vertexModule,
                entryPoint: "main",
                buffers
            },
            fragment: {
                module: fragmentModule,
                entryPoint: "main",
                targets
            },
            primitive: {
                topology: state.primitiveTopology,
                frontFace: state.frontFace,
                cullMode: state.cullMode
            }
        };

        if (state.depthState) {
            descriptor.depthStencil = {
                format: state.depthState.format,
                depthWriteEnabled: state.depthState.depthWriteEnabled,
                depthCompare: state.depthState.depthCompare,
                depthBias: state.depthState.depthBias,
                depthBiasSlopeScale: state.depthState.depthBiasSlopeScale
            };
        }

        const pipeline = foliumHostState.device.createRenderPipeline(descriptor);
        return storeResource("pipeline", pipeline);
    },

    destroyPipeline(pipelineToken) {
        requireResource(pipelineToken, "pipeline");
        resources.delete(pipelineToken);
    },

    setRenderPassPipeline(renderPassToken, pipelineToken) {
        const pass = requireResource(renderPassToken, "renderPass");
        const pipeline = requireResource(pipelineToken, "pipeline");
        pass.setPipeline(pipeline);
    },

    setRenderPassVertexBuffer(renderPassToken, slot, bufferToken, offset, length) {
        const pass = requireResource(renderPassToken, "renderPass");
        const buffer = requireResource(bufferToken, "buffer");
        pass.setVertexBuffer(slot, buffer, Number(offset), Number(length));
    },

    setRenderPassIndexBuffer(renderPassToken, bufferToken, indexFormat) {
        const pass = requireResource(renderPassToken, "renderPass");
        const buffer = requireResource(bufferToken, "buffer");
        pass.setIndexBuffer(buffer, indexFormat);
    },

    drawRenderPass(
        renderPassToken,
        vertexCount,
        instanceCount,
        firstVertex,
        firstInstance
    ) {
        requireResource(renderPassToken, "renderPass")
            .draw(vertexCount, instanceCount, firstVertex, firstInstance);
    },

    drawIndexedRenderPass(
        renderPassToken,
        indexCount,
        instanceCount,
        firstIndex,
        baseVertex,
        firstInstance
    ) {
        requireResource(renderPassToken, "renderPass")
            .drawIndexed(indexCount, instanceCount, firstIndex, baseVertex, firstInstance);
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
