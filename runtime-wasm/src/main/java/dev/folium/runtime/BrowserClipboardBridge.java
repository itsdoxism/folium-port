package dev.folium.runtime;

import org.teavm.jso.JSBody;

public final class BrowserClipboardBridge {
    static {
        install();
    }

    private BrowserClipboardBridge() {
    }

    @JSBody(script = """
        if (globalThis.__foliumClipboardInstalled) return;
        globalThis.__foliumClipboardInstalled = true;
        globalThis.__foliumClipboardText = globalThis.__foliumClipboardText || '';

        globalThis.addEventListener('copy', event => {
            const text = globalThis.__foliumClipboardText || '';
            try {
                event.clipboardData?.setData('text/plain', text);
                event.preventDefault();
            } catch (_) {}
        });

        globalThis.addEventListener('paste', event => {
            try {
                const text = event.clipboardData?.getData('text/plain');
                if (typeof text === 'string') {
                    globalThis.__foliumClipboardText = text;
                }
            } catch (_) {}
        });
    """)
    private static native void install();

    @JSBody(script = "return globalThis.__foliumClipboardText || '';")
    public static native String getClipboard();

    @JSBody(params = {"text"}, script = """
        const value = text || '';
        globalThis.__foliumClipboardText = value;

        try {
            const promise = navigator.clipboard?.writeText?.(value);
            if (promise?.catch) promise.catch(() => {});
        } catch (_) {}
    """)
    public static native void setClipboard(String text);

    @JSBody(script = """
        try {
            const promise = navigator.clipboard?.readText?.();
            if (promise?.then) {
                promise.then(text => {
                    if (typeof text === 'string') {
                        globalThis.__foliumClipboardText = text;
                    }
                }).catch(() => {});
            }
        } catch (_) {}
    """)
    public static native void refreshClipboard();
}
