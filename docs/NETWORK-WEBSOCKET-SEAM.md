# WebSocket network seam

Folium preserves Minecraft 26.3 packet/protocol semantics while replacing the socket transport.

## Transport stack

The browser session now owns:

```text
Packet<?> 
  -> ProtocolInfo.codec()
  -> BundlerInfo unbundle/bundle semantics
  -> optional compression envelope
  -> outer VarInt TCP frame
  -> optional AES/CFB8 stream encryption
  -> WebSocket raw byte tunnel
```

Inbound reverses that order.

## Packet bundling

Minecraft exposes bundling independently from Netty through:

```text
ProtocolInfo.bundlerInfo()
```

Folium reuses that object directly.

Outbound bundle packets are expanded with:

```text
BundlerInfo.unbundlePacket(packet, consumer)
```

which yields the normal delimiter/subpacket sequence expected on the wire.

Inbound decoded packets are fed through:

```text
BundlerInfo.startPacketBundling(packet)
BundlerInfo.Bundler.addPacket(packet)
```

until Minecraft's bundler returns the completed logical bundle packet.

This reproduces the behavior previously provided by `PacketBundleUnpacker` and `PacketBundlePacker` without using Netty channel handlers.

Terminal packets are rejected if they appear inside an active bundle, matching Minecraft's original guard behavior.

## Gateway

The gateway remains a raw TCP byte tunnel and does not know about packet bundling, compression, encryption or protocol state.
