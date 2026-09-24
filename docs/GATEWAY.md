# Folium WebSocket gateway

The Folium browser client cannot open a raw Minecraft TCP socket, so the gateway provides a same-origin WebSocket tunnel to one fixed Minecraft backend.

## Fixed backend

Configure the backend server-side:

```text
FOLIUM_BACKEND_HOST=127.0.0.1
FOLIUM_BACKEND_PORT=25565
```

The browser cannot choose arbitrary TCP targets.

## Listen address

```text
FOLIUM_GATEWAY_HOST=127.0.0.1
FOLIUM_GATEWAY_PORT=8081
```

The WebSocket endpoint is:

```text
/folium-gateway
```

## Raw byte tunnel

The gateway no longer parses Minecraft packet framing.

Each browser WebSocket binary message is simply a chunk of the Minecraft TCP byte stream.

Outbound:

```text
browser TCP-stream bytes
    -> WebSocket binary message
    -> gateway writes bytes directly to backend TCP socket
```

Inbound:

```text
backend TCP bytes
    -> gateway reads arbitrary chunk
    -> one WebSocket binary message
    -> browser stream accumulator
```

Minecraft outer VarInt framing, compression envelopes and encryption are all handled by the browser-side `FoliumNetworkSession`.

## Why raw tunneling matters

Minecraft online-mode encryption uses AES/CFB8 over the entire TCP stream, including the outer packet-length VarInt.

A gateway that parses VarInt frames would lose packet boundaries as soon as encryption starts.

Keeping the gateway byte-transparent means it does not need to know:

- protocol state;
- packet IDs;
- compression thresholds;
- encryption state;
- login transitions.

## Deployment

In production, reverse-proxy `/folium-gateway` from the same origin that serves the Folium browser client to the gateway process.
