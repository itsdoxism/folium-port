let translatorModule = null;

export async function prepareFoliumShaderTranslator() {
    if (translatorModule) {
        return translatorModule;
    }

    const module = await import(
        "../../shader-translator/pkg/folium_shader_translator.js"
    );

    await module.default(
        "../../shader-translator/pkg/folium_shader_translator_bg.wasm"
    );

    translatorModule = module;
    globalThis.__foliumShaderTranslator = {
        ready: true,
        glslToWgsl(source, stage) {
            return module.glsl_to_wgsl(source, stage);
        }
    };

    return translatorModule;
}

export function isFoliumShaderTranslatorReady() {
    return !!globalThis.__foliumShaderTranslator?.ready;
}
