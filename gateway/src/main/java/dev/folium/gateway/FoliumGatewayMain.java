package dev.folium.gateway;

import java.net.InetSocketAddress;

public final class FoliumGatewayMain {
    private FoliumGatewayMain() {
    }

    public static void main(String[] args) {
        String listenHost = env(
            "FOLIUM_GATEWAY_HOST",
            "127.0.0.1"
        );

        int listenPort = Integer.parseInt(
            env("FOLIUM_GATEWAY_PORT", "8081")
        );

        String backendHost = env(
            "FOLIUM_BACKEND_HOST",
            "127.0.0.1"
        );

        int backendPort = Integer.parseInt(
            env("FOLIUM_BACKEND_PORT", "25565")
        );

        FoliumGatewayServer server = new FoliumGatewayServer(
            new InetSocketAddress(listenHost, listenPort),
            backendHost,
            backendPort
        );

        server.start();
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank()
            ? fallback
            : value;
    }
}
