package dev.folium.render.webgpu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FoliumNetworkTrace {
    private static final List<String> EVENTS = new ArrayList<>();
    private static String stage = "IDLE";
    private static Throwable failure;

    private FoliumNetworkTrace() {
    }

    public static synchronized void reset() {
        EVENTS.clear();
        stage = "IDLE";
        failure = null;
    }

    public static synchronized void stage(String next) {
        stage = next;
        EVENTS.add("STAGE " + next);
        System.out.println("[Folium network] " + next);
    }

    public static synchronized void event(String message) {
        EVENTS.add(message);
        System.out.println("[Folium network] " + message);
    }

    public static synchronized void fail(
        String atStage,
        Throwable error
    ) {
        stage = atStage;
        failure = error;
        EVENTS.add(
            "FAIL " + atStage + ": " +
                error.getClass().getName() +
                ": " +
                error.getMessage()
        );

        error.printStackTrace(System.err);
    }

    public static synchronized String stage() {
        return stage;
    }

    public static synchronized Throwable failure() {
        return failure;
    }

    public static synchronized List<String> snapshot() {
        return Collections.unmodifiableList(
            new ArrayList<>(EVENTS)
        );
    }
}
