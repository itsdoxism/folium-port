package dev.folium.runtime;

import dev.folium.platform.WindowHost;
import org.teavm.jso.JSBody;

public final class BrowserWindowHost implements WindowHost {
    @Override
    @JSBody(script = """
        const canvas = document.getElementById('folium-canvas');
        if (!canvas) return 1;
        const dpr = globalThis.devicePixelRatio || 1;
        const width = Math.max(1, Math.round(canvas.clientWidth * dpr));
        if (canvas.width !== width) canvas.width = width;
        return width;
    """)
    public native int framebufferWidth();

    @Override
    @JSBody(script = """
        const canvas = document.getElementById('folium-canvas');
        if (!canvas) return 1;
        const dpr = globalThis.devicePixelRatio || 1;
        const height = Math.max(1, Math.round(canvas.clientHeight * dpr));
        if (canvas.height !== height) canvas.height = height;
        return height;
    """)
    public native int framebufferHeight();

    @Override
    @JSBody(script = """
        const canvas = document.getElementById('folium-canvas');
        return !!canvas && document.pointerLockElement === canvas;
    """)
    public native boolean pointerLocked();

    @Override
    @JSBody(script = """
        const canvas = document.getElementById('folium-canvas');
        if (canvas?.requestPointerLock) canvas.requestPointerLock();
    """)
    public native void requestPointerLock();

    @Override
    @JSBody(script = "if (document.exitPointerLock) document.exitPointerLock();")
    public native void releasePointerLock();

    @Override
    @JSBody(params = {"title"}, script = "document.title = title || 'Folium';")
    public native void setTitle(String title);

    @Override
    @JSBody(params = {"uri"}, script = """
        try {
            const opened = globalThis.open(uri, '_blank', 'noopener,noreferrer');
            if (!opened) {
                console.warn('Folium: browser blocked external URI', uri);
            }
        } catch (error) {
            console.warn('Folium: failed to open external URI', uri, error);
        }
    """)
    public native void openExternal(String uri);

    @Override
    @JSBody(params = {"message"}, script = """
        globalThis.alert?.(message || 'Minecraft error');
    """)
    public native void showError(String message);

    @Override
    @JSBody(params = {"message"}, script = """
        if (globalThis.confirm) {
            return !!globalThis.confirm(message || 'Minecraft error');
        }
        globalThis.alert?.(message || 'Minecraft error');
        return true;
    """)
    public native boolean showErrorWithContinue(String message);
}
