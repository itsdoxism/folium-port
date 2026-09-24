package dev.folium.runtime;

import dev.folium.platform.ClockHost;
import org.teavm.jso.JSBody;

public final class BrowserClockHost implements ClockHost {
    @Override
    @JSBody(script = "return Math.floor(performance.now() * 1000000);")
    public native long nanoTime();

    @Override
    @JSBody(script = "return Date.now();")
    public native long currentTimeMillis();
}
