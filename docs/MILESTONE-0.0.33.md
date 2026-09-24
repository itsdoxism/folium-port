# Milestone 0.0.33 — real login smoke harness and structured network tracing

Folium now has a focused login harness for exercising the browser networking stack independently from full client rendering.

## Added

```text
FoliumLoginSmokeHarness
FoliumNetworkTrace
```

## Harness behavior

The harness creates a patched Minecraft `Connection`, invokes Minecraft's own handshake/login setup, sends `ServerboundHelloPacket`, and then ticks the connection until it succeeds, disconnects, or hits an exception.

No packet IDs are hardcoded.

## Offline-mode target

On an offline-mode backend the harness can exercise:

```text
connect
handshake
login hello
compression negotiation
login finished
```

using the same packet codecs and `Connection` bridge as the real browser client.

## Online-mode target

On an online-mode backend the harness additionally reaches the encryption challenge and checks whether the TeaVM/JVM target can construct Minecraft's required JCE objects.

It intentionally stops at external session authentication when `shouldAuthenticate()` is true.

## Structured trace

Normal `FoliumConnectionBridge` traffic now records:

- remote target;
- inbound/outbound protocol changes;
- packet type sends;
- packet type receives;
- compression activation;
- encryption activation;
- failure stage and exception.

This turns the next browser test into an evidence-gathering step rather than another speculative patch pass.

## Current limitation

The current environment still cannot run the complete Minecraft 26.3 + TeaVM/JDK25 browser artifact end-to-end, so the harness is implemented but has not yet been executed against a live backend here.

The next blocker should be chosen from the first actual smoke trace failure. If the trace reaches `LOGIN_FINISHED`, networking is no longer the primary startup blocker.
