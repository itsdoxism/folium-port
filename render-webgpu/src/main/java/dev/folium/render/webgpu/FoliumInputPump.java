package dev.folium.render.webgpu;

import com.mojang.blaze3d.platform.Window;
import dev.folium.platform.FoliumRuntime;
import dev.folium.platform.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class FoliumInputPump {
    private FoliumInputPump() {
    }

    public static void poll(Minecraft minecraft, Window window) {
        long handle = window.handle();
        InputEvent event;

        while ((event = FoliumRuntime.platform().input().pollEvent()) != null) {
            switch (event.type()) {
                case KEY -> minecraft.keyboardHandler.keyPress(
                    handle,
                    event.action(),
                    new KeyEvent(
                        event.key(),
                        event.keycode(),
                        event.modifiers()
                    )
                );

                case TEXT -> {
                    if (event.text() != null && !event.text().isEmpty()) {
                        minecraft.keyboardHandler.textInput(handle, event.text());
                    }
                }

                case MOUSE_MOVE -> minecraft.mouseHandler.onMove(
                    handle,
                    event.x(),
                    event.y(),
                    event.dx(),
                    event.dy()
                );

                case MOUSE_BUTTON -> minecraft.mouseHandler.onButton(
                    handle,
                    new MouseButtonInfo(
                        event.button(),
                        event.modifiers()
                    ),
                    event.action()
                );

                case WHEEL -> minecraft.mouseHandler.onScroll(
                    handle,
                    event.wheelX(),
                    event.wheelY()
                );

                case POINTER_ENTER -> minecraft.mouseHandler.cursorEntered();

                case BLUR -> {
                    if (minecraft.mouseHandler.isMouseGrabbed()) {
                        minecraft.mouseHandler.releaseMouse();
                    }
                }

                case FOCUS -> {
                    // No dedicated 26.3 public focus callback exists here.
                    // Key/button state is already maintained by BrowserInputHost.
                }
            }
        }
    }

    public static void clear() {
        FoliumRuntime.platform().input().clearEvents();
    }
}
