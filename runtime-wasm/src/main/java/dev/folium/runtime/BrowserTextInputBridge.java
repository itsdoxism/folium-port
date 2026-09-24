package dev.folium.runtime;

import org.teavm.jso.JSBody;

public final class BrowserTextInputBridge {
    static {
        install();
    }

    private BrowserTextInputBridge() {
    }

    @JSBody(script = """
        if (globalThis.__foliumTextInputInstalled) return;
        globalThis.__foliumTextInputInstalled = true;
        globalThis.__foliumTextInputActive = false;

        const ensureSink = () => {
            let sink = document.getElementById('folium-text-input');
            if (sink) return sink;

            sink = document.createElement('textarea');
            sink.id = 'folium-text-input';
            sink.setAttribute('aria-hidden', 'true');
            sink.autocapitalize = 'off';
            sink.autocomplete = 'off';
            sink.spellcheck = false;

            Object.assign(sink.style, {
                position: 'fixed',
                left: '-10000px',
                top: '0',
                width: '1px',
                height: '1px',
                opacity: '0',
                pointerEvents: 'none'
            });

            document.body.appendChild(sink);

            sink.addEventListener('compositionend', event => {
                const text = event.data || '';
                if (text) {
                    globalThis.__foliumInputQueue?.push({
                        type: 2,
                        text
                    });
                }
                sink.value = '';
            });

            sink.addEventListener('input', event => {
                if (event.isComposing) return;
                const text = sink.value;
                if (text) {
                    globalThis.__foliumInputQueue?.push({
                        type: 2,
                        text
                    });
                    sink.value = '';
                }
            });

            return sink;
        };

        globalThis.__foliumEnsureTextInput = ensureSink;
        ensureSink();
    """)
    private static native void install();

    @JSBody(script = """
        globalThis.__foliumTextInputActive = true;
        const sink = globalThis.__foliumEnsureTextInput?.();
        sink?.focus({ preventScroll: true });
    """)
    public static native void start();

    @JSBody(script = """
        globalThis.__foliumTextInputActive = false;
        const sink = document.getElementById('folium-text-input');
        if (sink) {
            sink.value = '';
            sink.blur();
        }
        document.getElementById('folium-canvas')?.focus?.({ preventScroll: true });
    """)
    public static native void stop();

    @JSBody(script = "return !!globalThis.__foliumTextInputActive;")
    public static native boolean isActive();
}
