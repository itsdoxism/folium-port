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

    private int compressionThreshold = -1;
    private boolean validateDecompressed = true;

    public FoliumNetworkSession(String endpoint) {
        this.transport = FoliumRuntime.platform()
            .network()
            .connect(endpoint);
    }

    public boolean isOpen() {
        return transport.isOpen();
    }

    public boolean isClosed() {
        return transport.isClosed();
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

    public void setupCompression(
        int threshold,
        boolean validateDecompressed
    ) {
        this.compressionThreshold = threshold;
        this.validateDecompressed = validateDecompressed;
    }

    public int compressionThreshold() {
        return compressionThreshold;
    }

    public void send(Packet<?> packet) {
        ProtocolInfo<?> protocol = outboundProtocol;

        if (protocol == null) {
            throw new IllegalStateException(
                "Folium outbound protocol is not configured"
            );
        }

        byte[] packetPayload = FoliumPacketCodec.encode(
            protocol,
            packet
        );

        transport.send(
            FoliumCompressionCodec.encode(
                packetPayload,
                compressionThreshold
            )
        );
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

        byte[] packetPayload = FoliumCompressionCodec.decode(
            payload,
            compressionThreshold,
            validateDecompressed
        );

        return FoliumPacketCodec.decode(
            protocol,
            packetPayload
        );
    }

    @Override
    public void close() {
        transport.close();
    }
}
