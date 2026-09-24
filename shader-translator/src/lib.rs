use wasm_bindgen::prelude::*;

#[wasm_bindgen]
pub fn spv_to_wgsl(bytes: &[u8]) -> Result<String, JsValue> {
    let options = naga::front::spv::Options::default();

    let module = naga::front::spv::parse_u8_slice(bytes, &options)
        .map_err(|error| JsValue::from_str(&format!(
            "Folium SPIR-V parse failed: {error}"
        )))?;

    let mut validator = naga::valid::Validator::new(
        naga::valid::ValidationFlags::all(),
        naga::valid::Capabilities::all(),
    );

    let info = validator.validate(&module)
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
