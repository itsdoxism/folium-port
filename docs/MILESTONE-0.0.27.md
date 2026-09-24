# Milestone 0.0.27 — WebSocket transport and Minecraft codec reuse

Folium now has its first browser networking layer.

## Added browser transport

`BrowserNetworkHost` provides binary WebSocket connections through the platform API.

It supports:

- connect;
- open-state query;
- binary send;
- queued binary receive;
- close.

## Added packet codec bridge

`FoliumPacketCodec` reuses Minecraft 26.3's own:

```text
ProtocolInfo.codec()
```

to encode and decode packets using an in-memory Netty `ByteBuf`.

This avoids reimplementing packet IDs and packet field serialization.

## Added protocol-aware session

`FoliumNetworkSession` combines the browser transport with independent inbound/outbound protocol state.

```text
Packet<?> -> Minecraft codec -> byte[] -> WebSocket
WebSocket -> byte[] -> Minecraft codec -> Packet<?>
```

## Wire format

One WebSocket binary message equals one unframed Minecraft protocol packet payload.

The gateway is responsible for adding/removing the normal TCP VarInt frame length.

## Why this seam

Minecraft's `Connection` class is tightly coupled to Netty channels and event loops, but its actual packet serialization is not.

Separating codec reuse from transport replacement reduces the amount of Minecraft networking code that needs to be transformed.

## Current limitation

`Connection.send`, `Connection.tick`, protocol switching and incoming listener dispatch still use the Netty channel model.

The next milestone should patch those methods onto `FoliumNetworkSession` rather than replacing Minecraft packet/listener logic wholesale.
