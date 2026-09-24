# Minecraft Connection -> Folium WebSocket seam

Folium keeps Minecraft's `Connection` object because game code, listeners and protocol transitions already depend on it. The Netty channel is replaced only for remote browser connections.

## State storage

Browser transport state lives outside Minecraft's private fields in a `WeakHashMap<Connection, State>`.

Each state contains the remote `InetSocketAddress` and a `FoliumNetworkSession`.

## Connect

The patched `Connection.connectToServer(...)` creates a normal `Connection(CLIENTBOUND)`, preserves the remote address, opens a Folium WebSocket session and returns without creating a Netty channel or event loop.

The browser transport maps Minecraft connection requests to the same-origin WebSocket endpoint `/folium-gateway`.

Packets sent while the WebSocket is still connecting are queued and flushed after the `open` event.

## Protocol switching

`setupInboundProtocol(...)` keeps Minecraft listener validation and stores the listener on the `Connection`, but replaces Netty pipeline mutation with `FoliumConnectionBridge.setInboundProtocol(...)`.

`setupOutboundProtocol(...)` directly updates the session's outbound protocol.

The initial outbound protocol is `HandshakeProtocols.SERVERBOUND`.

## Compression

`Connection.setupCompression(int, boolean)` is patched onto `FoliumNetworkSession`.

Compression is applied after packet serialization and removed before packet decoding, matching Minecraft's Netty pipeline ordering:

```text
Packet
 -> ProtocolInfo codec
 -> compression envelope
 -> gateway outer frame
```

Inbound reverses that sequence.

## Send

The patched send path routes packets to:

```text
FoliumNetworkSession.send(Packet)
    -> ProtocolInfo.codec().encode(...)
    -> optional Minecraft compression envelope
    -> WebSocket binary message
```

The three public send overloads and private `sendPacket(...)` are redirected.

`ChannelFutureListener` completion callbacks are not yet modeled.

## Tick / receive

The patched `Connection.tick()` drains up to 4096 inbound packets per tick, removes the current compression envelope when enabled, decodes using the current inbound protocol, checks the active `PacketListener`, and dispatches through `Packet.handle(listener)`.

`TickablePacketListener.tick()` is still called once per connection tick.

## Current limitations

- packet bundling is not yet reproduced outside the Netty pipeline;
- encryption is not yet supported;
- `ChannelFutureListener` completion callbacks are ignored;
- disconnect reason propagation on remote close is incomplete;
- packet counters / bandwidth statistics are not updated by the bridge;
- local/in-memory server connections remain a separate desktop path.
