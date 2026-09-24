# WebSocket network seam

Minecraft 26.3 packet codecs are already separated from TCP framing.

The important contract is:

```text
ProtocolInfo.codec()
    -> StreamCodec<ByteBuf, Packet<?>>
```

`PacketEncoder` calls that codec directly, while VarInt frame encoding is a different Netty pipeline layer.

Folium therefore preserves Minecraft's existing packet codec and replaces only the transport/framing boundary.

## Browser transport

`BrowserNetworkHost` uses the browser `WebSocket` API in binary mode.

Each WebSocket message contains exactly one Minecraft **protocol packet payload**:

```text
packet id + packet fields
```

It does **not** contain the normal TCP VarInt packet-length prefix.

## Gateway contract

Outbound:

```text
Folium packet payload
      |
      v
WebSocket binary message
      |
      v
gateway
      |
      +-- prepend Minecraft VarInt frame length
      |
      v
normal Minecraft TCP backend
```

Inbound:

```text
Minecraft TCP stream
      |
      v
gateway VarInt frame decoder
      |
      v
one packet payload
      |
      v
one WebSocket binary message
      |
      v
Folium ProtocolInfo.codec().decode(...)
```

This keeps compression/encryption/framing concerns at the gateway boundary while Folium works with complete packet payloads.

## Codec reuse

`FoliumPacketCodec` uses:

```text
ProtocolInfo.codec().encode(ByteBuf, Packet)
ProtocolInfo.codec().decode(ByteBuf)
```

with an in-memory Netty `ByteBuf`.

This intentionally keeps Minecraft's generated/registered packet codecs unchanged.

## Session state

`FoliumNetworkSession` stores independent inbound and outbound `ProtocolInfo` references.

That mirrors Minecraft's protocol switching during:

```text
handshake
status/login
configuration
play
```

without depending on a Netty `ChannelPipeline`.

## Important remaining work

`Connection` still owns channel/event-loop state and protocol swaps. It has not yet been patched onto `FoliumNetworkSession`.

The next milestone is a browser-specific `Connection` transport bridge that replaces:

```text
channel.write / writeAndFlush
channel.isOpen
ChannelPipeline protocol reconfiguration
```

with session operations while keeping Minecraft's listeners, pending packet queue and protocol transition semantics.
