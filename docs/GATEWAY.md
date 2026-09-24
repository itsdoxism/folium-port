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

Folium sends one unframed Minecraft protocol packet payload per WebSocket binary message.

The gateway converts:

```text
WebSocket message
    -> VarInt payload length
    -> payload bytes
    -> Minecraft TCP stream
```

and reverses that process inbound.

## Packet size guard

The current maximum framed packet size is 8 MiB.

## Compression and encryption

The current gateway only understands the normal pre-compression VarInt packet framing.

Minecraft login can negotiate compression, and online-mode login can negotiate encryption. Those transitions are not implemented yet.

A real login smoke test therefore depends on adding compression handling and deciding where encryption terminates.
