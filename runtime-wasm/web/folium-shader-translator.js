let translatorModule = null;

export async function prepareFoliumShaderTranslator() {
    if (translatorModule) {
        return translatorModule;
    }

    try {
        const module = await import(
            "../../shader-translator/pkg/folium_shader_translator.js"
        );

        await module.default(
            "../../shader-translator/pkg/folium_shader_translator_bg.wasm"
        );

        translatorModule = module;
        globalThis.__foliumShaderTranslator = {
            ready: true,
            error: null,
            glslToWgsl(source, stage) {
                return module.glsl_to_wgsl(source, stage);
            }
        };

        return module;
    } catch (error) {
        console.warn("Folium shader translator unavailable", error);
        globalThis.__foliumShaderTranslator = {
            ready: false,
            error: String(error),
            glslToWgsl() {
                throw new Error(
                    "Folium shader translator is unavailable: " + String(error)
                );
            }
        };
        return null;
    }
}

export function isFoliumShaderTranslatorReady() {
    return !!globalThis.__foliumShaderTranslator?.ready;
}
