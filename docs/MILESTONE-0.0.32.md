# Milestone 0.0.32 — Minecraft packet bundling without Netty handlers

Folium now preserves Minecraft 26.3 packet bundle semantics outside the Netty pipeline.

## Original desktop path

Minecraft normally installs:

```text
PacketBundleUnpacker
PacketBundlePacker
```

as Netty channel handlers.

Those handlers are transport glue around the protocol-level `BundlerInfo` API.

## Folium path

`FoliumNetworkSession` now calls Minecraft's own:

```text
ProtocolInfo.bundlerInfo()
```

and reuses it directly.

### Outbound

A logical bundle packet is expanded by:

```text
BundlerInfo.unbundlePacket(...)
```

Each resulting physical packet is then serialized, compressed when enabled, framed and encrypted.

### Inbound

Decoded physical packets are passed through:

```text
BundlerInfo.startPacketBundling(...)
BundlerInfo.Bundler.addPacket(...)
```

until a completed logical bundle packet is returned to the normal Minecraft listener.

## Terminal packet guard

A terminal packet inside an active bundle throws an error, mirroring the original `PacketBundlePacker` safety check.

## Why no ASM patch was needed

Bundling is already exposed through `ProtocolInfo`, so no additional Minecraft class rewrite is required. The session can replace the Netty bundler handlers entirely at the transport boundary.

## Network status

```text
WebSocket raw tunnel       implemented
Minecraft packet codec     reused
outer TCP framing          implemented
Connection integration     implemented
compression                implemented
AES/CFB8 stream transport  implemented
packet bundling            implemented
real online login smoke    pending
```

The next useful milestone is a real login smoke harness. That should exercise handshake -> login -> optional encryption -> compression -> configuration using the current browser transport stack and report the first failing protocol transition rather than guessing the next blocker.
