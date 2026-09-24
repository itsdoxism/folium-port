# Milestone 0.0.28 — Minecraft Connection runs over Folium WebSocket sessions

Folium now patches Minecraft 26.3's remote `Connection` path away from Netty channels and onto the browser WebSocket transport.

## Patched Connection methods

```text
connectToServer(...)
setupInboundProtocol(...)
setupOutboundProtocol(...)
send(Packet)
send(Packet, ChannelFutureListener)
send(Packet, ChannelFutureListener, boolean)
sendPacket(...)
runOnceConnected(...)
tick()
disconnect(DisconnectionDetails)
isConnected()
isConnecting()
flushChannel()
setReadOnly()
isMemoryConnection()
```

Each target is matched by exact JVM descriptor in the patch verifier.

## Connection path

```text
Minecraft Connection
        |
        v
FoliumConnectionBridge
        |
        v
FoliumNetworkSession
        |
        +-- inbound ProtocolInfo
        +-- outbound ProtocolInfo
        |
        v
BrowserNetworkHost
        |
        v
WebSocket
```

## Async browser connect handling

Minecraft expects connection setup to proceed synchronously enough for handshake scheduling. Browser WebSockets are asynchronous.

Folium solves this by allowing packet sends while the socket is still `CONNECTING`: binary payloads are stored in an outbound queue and flushed when the WebSocket fires `open`.

That lets Minecraft's handshake code run immediately without a Netty event loop.

## Receive path

Inbound binary messages are decoded with Minecraft's current `ProtocolInfo.codec()` and dispatched directly to the existing packet listener.

Minecraft packet classes and listeners are not rewritten.

## Gateway convention

A Minecraft address becomes:

```text
minecraft://example.org:25565
```

inside the Java transport layer, then the browser host maps it to:

```text
/folium-gateway?host=example.org&port=25565
```

on the current origin using `ws:` or `wss:` to match the page scheme.

## Current status

```text
WebSocket transport       implemented
Minecraft packet codec    reused
Connection send           patched
Connection receive/tick   patched
protocol switching        patched
async pre-open send queue implemented
compression               pending
encryption                pending
packet bundling           pending
gateway server            pending
browser runtime smoke     pending
```

The next milestone should implement the gateway framing layer and then add compression handling at a clearly defined side of that boundary before attempting a real login handshake.
