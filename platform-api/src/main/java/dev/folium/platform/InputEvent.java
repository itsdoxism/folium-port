package dev.folium.platform;

public record InputEvent(
    Type type,
    int action,
    int key,
    int keycode,
    int modifiers,
    int button,
    double x,
    double y,
    double dx,
    double dy,
    double wheelX,
    double wheelY,
    String text
) {
    public enum Type {
        KEY,
        TEXT,
        MOUSE_MOVE,
        MOUSE_BUTTON,
        WHEEL,
        POINTER_ENTER,
        FOCUS,
        BLUR
    }

    public static InputEvent key(
        int action,
        int key,
        int keycode,
        int modifiers
    ) {
        return new InputEvent(
            Type.KEY, action, key, keycode, modifiers, 0,
            0, 0, 0, 0, 0, 0, null
        );
    }

    public static InputEvent text(String text) {
        return new InputEvent(
            Type.TEXT, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, text
        );
    }

    public static InputEvent mouseMove(
        double x,
        double y,
        double dx,
        double dy
    ) {
        return new InputEvent(
            Type.MOUSE_MOVE, 0, 0, 0, 0, 0,
            x, y, dx, dy, 0, 0, null
        );
    }

    public static InputEvent mouseButton(
        int action,
        int button,
        int modifiers
    ) {
        return new InputEvent(
            Type.MOUSE_BUTTON, action, 0, 0, modifiers, button,
            0, 0, 0, 0, 0, 0, null
        );
    }

    public static InputEvent wheel(double x, double y) {
        return new InputEvent(
            Type.WHEEL, 0, 0, 0, 0, 0,
            0, 0, 0, 0, x, y, null
        );
    }

    public static InputEvent simple(Type type) {
        return new InputEvent(
            type, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, null
        );
    }
}
