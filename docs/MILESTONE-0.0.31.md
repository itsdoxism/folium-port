# Milestone 0.0.31 — raw TCP tunneling and online-mode stream encryption

Folium now models the browser network connection as a real Minecraft TCP byte stream rather than packet-shaped WebSocket messages.

## Gateway refactor

The gateway is now a raw byte tunnel.

It no longer reads or writes Minecraft VarInt packet lengths and no longer has a `VarInt` framing helper.

This is required because Minecraft encryption covers the complete TCP stream, including packet-length bytes.

## Browser framing

`FoliumTcpFrameCodec` now owns the outer Minecraft VarInt frame.

`FoliumNetworkSession` maintains an inbound stream accumulator so arbitrary WebSocket chunks can contain:

- partial frame headers;
- partial packet payloads;
- multiple complete frames.

## Encryption

`FoliumNetworkSession` now stores Minecraft's decrypt/encrypt `Cipher` instances.

When enabled:

```text
outbound: frame -> AES/CFB8 update -> WebSocket
inbound:  WebSocket -> AES/CFB8 update -> frame decoder
```

This preserves continuous CFB8 state across transport chunks.

## Patched Minecraft methods

```text
Connection.setEncryptionKey(Cipher, Cipher)
ClientHandshakePacketListenerImpl.setEncryption(
    ServerboundKeyPacket,
    Cipher,
    Cipher
)
```

The login listener patch preserves the critical ordering:

1. send the encryption-response packet while the stream is still plaintext;
2. enable the ciphers immediately afterward;
3. all later TCP bytes are encrypted.

## Tests added

- `FoliumTcpFrameCodecTest` checks partial-frame handling and preservation of remaining stream bytes;
- `FoliumCipherStreamTest` checks that AES/CFB8 cipher state remains continuous across multiple chunks.

## Current network status

```text
WebSocket raw tunnel       implemented
Minecraft packet codec     reused
outer TCP framing          implemented
Connection integration     implemented
compression                implemented
AES/CFB8 stream transport  implemented
login cipher ordering      implemented
JCE-on-TeaVM viability     unverified
packet bundling            pending
real online login smoke    pending
```

The next step should audit packet bundling and then attempt the first real login smoke path. If TeaVM cannot supply Minecraft's required JCE primitives, the next blocker becomes a browser-compatible implementation of the login cryptography helpers rather than the transport itself.
