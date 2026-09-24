package dev.folium.render.webgpu;

import dev.folium.platform.FoliumRuntime;
import dev.folium.platform.NetworkHost;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;

import java.util.Objects;

public final class FoliumNetworkSession implements AutoCloseable {
    private final NetworkHost.Connection transport;

    private ProtocolInfo<?> inboundProtocol;
    private ProtocolInfo<?> outboundProtocol;

    public FoliumNetworkSession(String endpoint) {
        this.transport = FoliumRuntime.platform()
            .network()
            .connect(endpoint);
    }

    public boolean isOpen() {
        return transport.isOpen();
    }

    public void setInboundProtocol(ProtocolInfo<?> protocol) {
        this.inboundProtocol = Objects.requireNonNull(
            protocol,
            "protocol"
        );
    }

    public void setOutboundProtocol(ProtocolInfo<?> protocol) {
        this.outboundProtocol = Objects.requireNonNull(
            protocol,
            "protocol"
        );
    }

    public ProtocolInfo<?> inboundProtocol() {
        return inboundProtocol;
    }

    public ProtocolInfo<?> outboundProtocol() {
        return outboundProtocol;
    }

    public void send(Packet<?> packet) {
        ProtocolInfo<?> protocol = outboundProtocol;

        if (protocol == null) {
            throw new IllegalStateException(
                "Folium outbound protocol is not configured"
            );
        }

        transport.send(FoliumPacketCodec.encode(protocol, packet));
    }

    public Packet<?> pollPacket() {
        byte[] payload = transport.poll();
        if (payload == null) {
            return null;
        }

        ProtocolInfo<?> protocol = inboundProtocol;

        if (protocol == null) {
            throw new IllegalStateException(
                "Folium inbound protocol is not configured"
            );
        }

        return FoliumPacketCodec.decode(protocol, payload);
    }

    @Override
    public void close() {
        transport.close();
    }
}
