# Milestone 0.0.29 — WebSocket to Minecraft TCP gateway

Folium now includes a gateway module that converts browser WebSocket packet messages into normal Minecraft TCP framing.

## Added module

```text
gateway/
  build.gradle
  src/main/java/dev/folium/gateway/
    FoliumGatewayMain.java
    FoliumGatewayServer.java
    VarInt.java
```

## Wire conversion

Outbound:

```text
browser WebSocket binary message
        |
        v
VarInt payload length + payload
        |
        v
Minecraft TCP backend
```

Inbound:

```text
Minecraft TCP stream
        |
        v
read VarInt packet length
        |
        v
read exact payload bytes
        |
        v
one browser WebSocket binary message
```

## Security model

The gateway connects only to a backend configured by environment variables.

The browser cannot choose arbitrary TCP hosts or ports.

## Browser routing

A Folium `minecraft://...` transport request now maps to the same-origin WebSocket path:

```text
/folium-gateway
```

The actual backend address stays server-side.

## Tests

A JUnit test now exercises representative Minecraft VarInt frame lengths, including 0, 127, 128, 255 and the current 8 MiB packet guard boundary.

The current execution environment still does not have a usable Gradle/JDK 25 toolchain for a full project integration run, so the gateway has not been end-to-end launched here.

## Next blocker

Minecraft protocol state can enable compression during login.

The next milestone should add compression framing so the gateway can switch from:

```text
length + packet
```

to Minecraft's compressed packet envelope without changing the browser-side packet codec contract.
