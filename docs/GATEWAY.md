# Folium WebSocket gateway

The Folium browser client cannot open a raw Minecraft TCP socket.

The gateway accepts binary WebSocket messages and forwards them to one configured Minecraft backend.

## Fixed backend

The gateway intentionally does not accept arbitrary `host` or `port` query parameters.

Configure the backend server-side:

```text
FOLIUM_BACKEND_HOST=127.0.0.1
FOLIUM_BACKEND_PORT=25565
```

This prevents the gateway from becoming an open TCP proxy.

## Listen address

```text
FOLIUM_GATEWAY_HOST=127.0.0.1
FOLIUM_GATEWAY_PORT=8081
```

The WebSocket endpoint is:

```text
/folium-gateway
```

In production, reverse-proxy that path from the same origin that serves Folium.

## Framing

The gateway only owns the **outer Minecraft TCP packet length**.

Outbound:

```text
WebSocket message
    -> VarInt frame length
    -> message bytes
    -> Minecraft TCP stream
```

Inbound reverses that process.

The gateway does not parse packet IDs and does not need to know the current Minecraft protocol state.

## Compression boundary

When Minecraft login enables compression, the WebSocket message itself becomes Minecraft's compression envelope:

```text
VarInt uncompressedLength
compressed-or-raw packet payload
```

Folium handles that envelope in `FoliumNetworkSession`.

The gateway still sees only opaque message bytes and continues to add/remove the outer TCP frame length.

This keeps compression negotiation synchronized with the patched Minecraft `Connection.setupCompression(int, boolean)` method without making the gateway protocol-aware.

## Packet size guard

The current maximum outer framed message size is 8 MiB.

## Encryption

Online-mode login can negotiate stream encryption. That transition is not implemented yet.

The next networking boundary is deciding whether encryption terminates in the browser or in a trusted gateway-side login bridge.
