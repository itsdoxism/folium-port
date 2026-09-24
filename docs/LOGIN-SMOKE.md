# Folium login smoke harness

The smoke harness exercises the networking stack without depending on the full renderer/UI boot.

## Entry point

```text
FoliumLoginSmokeHarness.start(
    handshakeHost,
    port,
    username
)
```

Call `tick()` repeatedly until `isStopped()` becomes true.

The harness uses Minecraft's own:

```text
Connection.connectToServer(...)
Connection.initiateServerboundPlayConnection(...)
ServerboundHelloPacket
```

rather than manually constructing packet IDs or protocol bytes.

## Expected offline-mode sequence

A healthy offline-mode login should progress roughly through:

```text
CONNECT
HANDSHAKE
OUTBOUND_PROTOCOL_LOGIN
INBOUND_PROTOCOL_LOGIN
LOGIN_HELLO_SEND
recv login/compression       (optional)
COMPRESSION_<threshold>      (optional)
recv login/finished
LOGIN_FINISHED
```

Exact packet type names are also written to the live trace.

## Online-mode sequence

If the server requests encryption:

```text
ENCRYPTION_CHALLENGE
```

The harness attempts to create:

- the AES shared secret;
- the RSA-encrypted key response packet;
- AES/CFB8 encrypt/decrypt `Cipher` instances.

If those operations succeed, the trace reports that the JCE primitives are available.

When the server requires Mojang/Microsoft session authentication, the standalone smoke harness stops at:

```text
SESSION_AUTH_REQUIRED
```

That boundary is deliberate: account/session authentication belongs to the real client login flow and should not be faked by the transport harness.

## Live trace

`FoliumConnectionBridge` now emits structured stage and packet events for normal connections as well:

```text
STAGE CONNECT
remote=...
STAGE OUTBOUND_PROTOCOL_...
STAGE INBOUND_PROTOCOL_...
send <packet type>
recv <packet type>
STAGE COMPRESSION_...
STAGE ENCRYPTION_ENABLED
```

Failures are recorded with the stage active at the time of the exception.

## What the harness proves

It is intended to identify the first broken transition among:

```text
WebSocket connect
handshake encode/send
login protocol switch
login hello
packet decode
compression activation
JCE crypto construction
encryption stream activation
login finished
```

It does not prove renderer or game-state initialization.
