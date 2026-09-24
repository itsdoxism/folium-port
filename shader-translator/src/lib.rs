use wasm_bindgen::prelude::*;

fn shader_stage(stage: &str) -> Result<naga::ShaderStage, JsValue> {
    match stage {
        "vertex" => Ok(naga::ShaderStage::Vertex),
        "fragment" => Ok(naga::ShaderStage::Fragment),
        other => Err(JsValue::from_str(&format!(
            "Folium shader stage is unsupported: {other}"
        ))),
    }
}

#[wasm_bindgen]
pub fn glsl_to_wgsl(source: &str, stage: &str) -> Result<String, JsValue> {
    let stage = shader_stage(stage)?;
    let mut frontend = naga::front::glsl::Frontend::default();
    let options = naga::front::glsl::Options::from(stage);

    let module = frontend
        .parse(&options, source)
        .map_err(|errors| JsValue::from_str(&format!(
            "Folium GLSL parse failed: {errors:?}"
        )))?;

    let mut validator = naga::valid::Validator::new(
        naga::valid::ValidationFlags::all(),
        naga::valid::Capabilities::all(),
    );

    let info = validator
        .validate(&module)
        .map_err(|error| JsValue::from_str(&format!(
            "Folium shader validation failed: {error}"
        )))?;

    naga::back::wgsl::write_string(
        &module,
        &info,
        naga::back::wgsl::WriterFlags::empty(),
    )
    .map_err(|error| JsValue::from_str(&format!(
        "Folium WGSL generation failed: {error}"
    )))
}
