# Minecraft Connection -> Folium WebSocket seam

Folium keeps Minecraft's `Connection` object and replaces its remote Netty channel with `FoliumNetworkSession`.

## Browser TCP stream model

The browser session now owns the complete Minecraft TCP transport stack:

```text
Packet
  -> ProtocolInfo codec
  -> optional compression envelope
  -> outer VarInt frame
  -> optional AES/CFB8 stream encryption
  -> WebSocket raw byte tunnel
```

Inbound reverses that exact order.

## Outer framing

`FoliumTcpFrameCodec` adds and removes Minecraft's outer VarInt packet length.

Inbound WebSocket chunks are accumulated because TCP read boundaries and WebSocket chunk boundaries are not treated as packet boundaries.

The frame decoder waits until a complete VarInt header and complete payload are available before returning a packet envelope.

## Compression

`Connection.setupCompression(int, boolean)` updates the session compression threshold.

Compression is applied inside the outer TCP frame, matching Minecraft's normal pipeline ordering.

## Online-mode encryption

`Connection.setEncryptionKey(Cipher decrypt, Cipher encrypt)` now installs the two Minecraft `AES/CFB8/NoPadding` ciphers directly into the Folium session.

After activation:

```text
outbound full TCP frame -> encryptCipher.update(...)
inbound raw TCP bytes   -> decryptCipher.update(...)
```

Cipher objects are stateful and remain continuous across WebSocket chunks.

## Login activation ordering

Minecraft normally sends `ServerboundKeyPacket` with a `ChannelFutureListener` callback that enables encryption only after the key-response packet has been written.

Folium does not have a Netty `ChannelFuture`, so the patched `ClientHandshakePacketListenerImpl.setEncryption(...)` performs the equivalent sequence synchronously:

```text
send ServerboundKeyPacket using plaintext framing
then
Connection.setEncryptionKey(...)
```

`BrowserNetworkHost.send(...)` copies/queues the already encoded bytes, so later enabling the cipher cannot retroactively encrypt the key-response packet.

## Remaining crypto risk

The transport side of AES/CFB8 is implemented, but Minecraft's login handshake still relies on Java cryptography for:

- AES secret-key generation;
- SHA-1 server digest;
- RSA encryption of the shared secret/challenge;
- creation of the AES/CFB8 cipher objects.

Whether the TeaVM/WASM target provides all required JCE primitives is still runtime-unverified.
