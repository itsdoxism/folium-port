# Milestone 0.0.30 — Minecraft compression envelope support

Folium now handles Minecraft 26.3 login compression without making the WebSocket gateway protocol-aware.

## Compression state

`FoliumNetworkSession` now stores:

```text
compressionThreshold = -1   // disabled
validateDecompressed = true
```

The patched:

```text
Connection.setupCompression(int threshold, boolean validate)
```

updates that session state directly.

## Outbound encoding

Compression disabled:

```text
packet payload
```

Compression enabled, payload below threshold:

```text
VarInt 0
raw packet payload
```

Compression enabled, payload at or above threshold:

```text
VarInt originalLength
zlib-deflated packet payload
```

The implementation follows Minecraft 26.3's `CompressionEncoder` behavior, including the 8 MiB maximum packet guard.

## Inbound decoding

For compressed traffic, Folium reads the leading uncompressed-length VarInt.

- `0` means the remaining bytes are already an uncompressed packet payload;
- a positive length means the remaining bytes are inflated with zlib;
- when validation is enabled, a compressed packet below the negotiated threshold is rejected;
- expanded payloads above 8 MiB are rejected.

The resulting packet payload is then passed to the existing `ProtocolInfo.codec()` decoder.

## Ordering guarantee

The gateway preserves WebSocket message order.

During login:

```text
Set Compression packet
    -> decoded with compression disabled
    -> listener calls Connection.setupCompression(...)
    -> session threshold changes
    -> next queued WebSocket frame is decoded with compression enabled
```

So the gateway does not need to inspect the Set Compression packet itself.

## Tests

`FoliumCompressionCodecTest` covers:

- raw packet below threshold;
- compressed packet at/above threshold;
- disabled-compression identity behavior.

The full Gradle/JDK25 test suite has not been executed in the current environment, so this milestone is implemented but still runtime-unverified.

## Network status

```text
WebSocket transport       implemented
Minecraft packet codec    reused
Connection integration    implemented
TCP gateway framing       implemented
compression negotiation   implemented
encryption                 pending
packet bundling            pending
real login smoke           pending
```

The next major blocker is encryption for online-mode login, followed by a real handshake/login smoke test.
