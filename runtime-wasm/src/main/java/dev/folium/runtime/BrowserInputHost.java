package dev.folium.runtime;

import dev.folium.platform.InputEvent;
import dev.folium.platform.InputHost;
import org.teavm.jso.JSBody;

public final class BrowserInputHost implements InputHost {
    public BrowserInputHost() {
        installListeners();
    }

    @JSBody(script = """
        if (globalThis.__foliumInputInstalled) return;
        globalThis.__foliumInputInstalled = true;

        const queue = globalThis.__foliumInputQueue = [];
        const keys = globalThis.__foliumKeysDown = new Set();
        const buttons = globalThis.__foliumMouseButtons = new Set();
        const mouse = globalThis.__foliumMouseState = {
            x: 0, y: 0, wheelX: 0, wheelY: 0
        };

        const codeToScan = code => {
            if (/^Key[A-Z]$/.test(code)) return 4 + code.charCodeAt(3) - 65;
            if (/^Digit[1-9]$/.test(code)) return 30 + Number(code.slice(5)) - 1;
            if (code === 'Digit0') return 39;
            if (/^F([1-9]|1[0-2])$/.test(code)) return 57 + Number(code.slice(1));

            return ({
                Enter: 40,
                Escape: 41,
                Backspace: 42,
                Tab: 43,
                Space: 44,
                Minus: 45,
                Equal: 46,
                BracketLeft: 47,
                BracketRight: 48,
                Backslash: 49,
                Semicolon: 51,
                Quote: 52,
                Backquote: 53,
                Comma: 54,
                Period: 55,
                Slash: 56,
                CapsLock: 57,
                PrintScreen: 70,
                ScrollLock: 71,
                Pause: 72,
                Insert: 73,
                Home: 74,
                PageUp: 75,
                Delete: 76,
                End: 77,
                PageDown: 78,
                ArrowRight: 79,
                ArrowLeft: 80,
                ArrowDown: 81,
                ArrowUp: 82,
                NumLock: 83,
                NumpadDivide: 84,
                NumpadMultiply: 85,
                NumpadSubtract: 86,
                NumpadAdd: 87,
                NumpadEnter: 88,
                Numpad1: 89,
                Numpad2: 90,
                Numpad3: 91,
                Numpad4: 92,
                Numpad5: 93,
                Numpad6: 94,
                Numpad7: 95,
                Numpad8: 96,
                Numpad9: 97,
                Numpad0: 98,
                NumpadDecimal: 99,
                ControlLeft: 224,
                ShiftLeft: 225,
                AltLeft: 226,
                MetaLeft: 227,
                ControlRight: 228,
                ShiftRight: 229,
                AltRight: 230,
                MetaRight: 231
            })[code] || 0;
        };

        const keyCode = event => {
            if (event.key && event.key.length === 1) {
                return event.key.toLowerCase().codePointAt(0);
            }

            return ({
                Enter: 13,
                Backspace: 8,
                Tab: 9,
                Delete: 127,
                ArrowLeft: 1073741904,
                ArrowRight: 1073741903,
                ArrowUp: 1073741906,
                ArrowDown: 1073741905,
                Home: 1073741898,
                End: 1073741901,
                PageUp: 1073741899,
                PageDown: 1073741902,
                F5: 1073741886,
                Control: 1073742048,
                ' ': 32
            })[event.key] || 0;
        };

        const modifiers = event => {
            let value = 0;
            if (event.shiftKey) value |= 3;
            if (event.ctrlKey) value |= 192;
            if (event.altKey) value |= 768;
            if (event.metaKey) value |= 3072;
            if (event.getModifierState?.('NumLock')) value |= 4096;
            if (event.getModifierState?.('CapsLock')) value |= 8192;
            return value;
        };

        const canvasPoint = event => {
            const canvas = document.getElementById('folium-canvas');
            if (!canvas) return [0, 0, event.movementX || 0, event.movementY || 0];

            const rect = canvas.getBoundingClientRect();
            const sx = rect.width > 0 ? canvas.width / rect.width : 1;
            const sy = rect.height > 0 ? canvas.height / rect.height : 1;

            return [
                (event.clientX - rect.left) * sx,
                (event.clientY - rect.top) * sy,
                (event.movementX || 0) * sx,
                (event.movementY || 0) * sy
            ];
        };

        globalThis.addEventListener('keydown', event => {
            const scan = codeToScan(event.code);
            if (scan) keys.add(scan);

            queue.push({
                type: 1,
                action: event.repeat ? -1 : 1,
                key: scan,
                keycode: keyCode(event),
                modifiers: modifiers(event)
            });

            if (
                event.key?.length === 1 &&
                !event.ctrlKey &&
                !event.metaKey &&
                !event.altKey
            ) {
                queue.push({ type: 2, text: event.key });
            }
        });

        globalThis.addEventListener('keyup', event => {
            const scan = codeToScan(event.code);
            if (scan) keys.delete(scan);

            queue.push({
                type: 1,
                action: 0,
                key: scan,
                keycode: keyCode(event),
                modifiers: modifiers(event)
            });
        });

        globalThis.addEventListener('blur', () => {
            keys.clear();
            buttons.clear();
            queue.push({ type: 8 });
        });

        globalThis.addEventListener('focus', () => {
            queue.push({ type: 7 });
        });

        const canvas = document.getElementById('folium-canvas');
        if (canvas) {
            canvas.addEventListener('pointerenter', () => {
                queue.push({ type: 6 });
            });

            canvas.addEventListener('pointermove', event => {
                const [x, y, dx, dy] = canvasPoint(event);
                mouse.x = x;
                mouse.y = y;
                queue.push({ type: 3, x, y, dx, dy });
            });

            canvas.addEventListener('pointerdown', event => {
                const button = event.button === 0 ? 1 :
                    event.button === 1 ? 2 :
                    event.button === 2 ? 3 : event.button + 1;
                buttons.add(button);
                queue.push({
                    type: 4,
                    action: 1,
                    button,
                    modifiers: modifiers(event)
                });
                event.preventDefault();
            });

            canvas.addEventListener('pointerup', event => {
                const button = event.button === 0 ? 1 :
                    event.button === 1 ? 2 :
                    event.button === 2 ? 3 : event.button + 1;
                buttons.delete(button);
                queue.push({
                    type: 4,
                    action: 0,
                    button,
                    modifiers: modifiers(event)
                });
                event.preventDefault();
            });

            canvas.addEventListener('contextmenu', event => {
                event.preventDefault();
            });

            canvas.addEventListener('wheel', event => {
                const scale = event.deltaMode === 1 ? 1 :
                    event.deltaMode === 2 ? 10 : 0.01;
                const wheelX = -event.deltaX * scale;
                const wheelY = -event.deltaY * scale;
                mouse.wheelX += wheelX;
                mouse.wheelY += wheelY;
                queue.push({ type: 5, wheelX, wheelY });
                event.preventDefault();
            }, { passive: false });
        }
    """)
    private static native void installListeners();

    @Override
    @JSBody(params = {"keyCode"}, script = "return !!globalThis.__foliumKeysDown?.has(keyCode);")
    public native boolean keyDown(int keyCode);

    @Override
    @JSBody(params = {"button"}, script = "return !!globalThis.__foliumMouseButtons?.has(button);")
    public native boolean mouseButtonDown(int button);

    @Override
    @JSBody(script = "return globalThis.__foliumMouseState?.x || 0;")
    public native double mouseX();

    @Override
    @JSBody(script = "return globalThis.__foliumMouseState?.y || 0;")
    public native double mouseY();

    @Override
    @JSBody(script = """
        const state = globalThis.__foliumMouseState;
        if (!state) return 0;
        const value = state.wheelX;
        state.wheelX = 0;
        return value;
    """)
    public native double wheelDeltaX();

    @Override
    @JSBody(script = """
        const state = globalThis.__foliumMouseState;
        if (!state) return 0;
        const value = state.wheelY;
        state.wheelY = 0;
        return value;
    """)
    public native double wheelDeltaY();

    @Override
    public InputEvent pollEvent() {
        int type = beginPoll();
        return switch (type) {
            case 1 -> InputEvent.key(
                currentAction(),
                currentKey(),
                currentKeycode(),
                currentModifiers()
            );
            case 2 -> InputEvent.text(currentText());
            case 3 -> InputEvent.mouseMove(
                currentX(), currentY(), currentDx(), currentDy()
            );
            case 4 -> InputEvent.mouseButton(
                currentAction(),
                currentButton(),
                currentModifiers()
            );
            case 5 -> InputEvent.wheel(currentWheelX(), currentWheelY());
            case 6 -> InputEvent.simple(InputEvent.Type.POINTER_ENTER);
            case 7 -> InputEvent.simple(InputEvent.Type.FOCUS);
            case 8 -> InputEvent.simple(InputEvent.Type.BLUR);
            default -> null;
        };
    }

    @Override
    @JSBody(script = """
        if (globalThis.__foliumInputQueue) globalThis.__foliumInputQueue.length = 0;
        globalThis.__foliumCurrentInputEvent = null;
    """)
    public native void clearEvents();

    @Override
    public void startTextInput() {
        BrowserTextInputBridge.start();
    }

    @Override
    public void stopTextInput() {
        BrowserTextInputBridge.stop();
    }

    @Override
    public boolean textInputActive() {
        return BrowserTextInputBridge.isActive();
    }

    @Override
    public String getClipboard() {
        BrowserClipboardBridge.refreshClipboard();
        return BrowserClipboardBridge.getClipboard();
    }

    @Override
    public void setClipboard(String text) {
        BrowserClipboardBridge.setClipboard(text);
    }

    @JSBody(script = """
        const queue = globalThis.__foliumInputQueue;
        const event = queue?.shift() || null;
        globalThis.__foliumCurrentInputEvent = event;
        return event?.type || 0;
    """)
    private static native int beginPoll();

    @JSBody(script = "return globalThis.__foliumCurrentInputEvent?.action || 0;")
    private static native int currentAction();

    @JSBody(script = "return globalThis.__foliumCurrentInputEvent?.key || 0;")
    private static native int currentKey();

    @JSBody(script = "return globalThis.__foliumCurrentInputEvent?.keycode || 0;")
    private static native int currentKeycode();

    @JSBody(script = "return globalThis.__foliumCurrentInputEvent?.modifiers || 0;")
    private static native int currentModifiers();

    @JSBody(script = "return globalThis.__foliumCurrentInputEvent?.button || 0;")
    private static native int currentButton();

    @JSBody(script = "return globalThis.__foliumCurrentInputEvent?.x || 0;")
    private static native double currentX();

    @JSBody(script = "return globalThis.__foliumCurrentInputEvent?.y || 0;")
    private static native double currentY();

    @JSBody(script = "return globalThis.__foliumCurrentInputEvent?.dx || 0;")
    private static native double currentDx();

    @JSBody(script = "return globalThis.__foliumCurrentInputEvent?.dy || 0;")
    private static native double currentDy();

    @JSBody(script = "return globalThis.__foliumCurrentInputEvent?.wheelX || 0;")
    private static native double currentWheelX();

    @JSBody(script = "return globalThis.__foliumCurrentInputEvent?.wheelY || 0;")
    private static native double currentWheelY();

    @JSBody(script = "return globalThis.__foliumCurrentInputEvent?.text || '';")
    private static native String currentText();
}
