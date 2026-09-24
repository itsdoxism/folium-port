package dev.folium.render.webgpu;

import dev.folium.platform.FoliumRuntime;
import dev.folium.platform.NetworkHost;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;

import javax.crypto.Cipher;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Objects;
import java.util.Queue;

public final class FoliumNetworkSession implements AutoCloseable {
    private final NetworkHost.Connection transport;

    private ProtocolInfo<?> inboundProtocol;
    private ProtocolInfo<?> outboundProtocol;

    private int compressionThreshold = -1;
    private boolean validateDecompressed = true;

    private Cipher decryptCipher;
    private Cipher encryptCipher;

    private byte[] inboundStream = new byte[0];

    private BundlerInfo.Bundler inboundBundler;
    private final Queue<Packet<?>> readyInboundPackets =
        new ArrayDeque<>();

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
        this.inboundBundler = null;
        this.readyInboundPackets.clear();
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

    public void setEncryptionKey(
        Cipher decryptCipher,
        Cipher encryptCipher
    ) {
        this.decryptCipher = Objects.requireNonNull(
            decryptCipher,
            "decryptCipher"
        );
        this.encryptCipher = Objects.requireNonNull(
            encryptCipher,
            "encryptCipher"
        );
    }

    public boolean encryptionEnabled() {
        return decryptCipher != null && encryptCipher != null;
    }

    public void send(Packet<?> packet) {
        ProtocolInfo<?> protocol = outboundProtocol;

        if (protocol == null) {
            throw new IllegalStateException(
                "Folium outbound protocol is not configured"
            );
        }

        protocol.bundlerInfo().unbundlePacket(
            packet,
            this::sendPhysicalPacket
        );
    }

    private void sendPhysicalPacket(Packet<?> packet) {
        ProtocolInfo<?> protocol = outboundProtocol;

        if (protocol == null) {
            throw new IllegalStateException(
                "Folium outbound protocol changed during bundle send"
            );
        }

        byte[] packetPayload = FoliumPacketCodec.encode(
            protocol,
            packet
        );

        byte[] compressionEnvelope = FoliumCompressionCodec.encode(
            packetPayload,
            compressionThreshold
        );

        byte[] tcpFrame = FoliumTcpFrameCodec.frame(
            compressionEnvelope
        );

        if (encryptCipher != null) {
            tcpFrame = updateCipher(
                encryptCipher,
                tcpFrame,
                "encrypt"
            );
        }

        transport.send(tcpFrame);
    }

    public Packet<?> pollPacket() {
        Packet<?> ready = readyInboundPackets.poll();
        if (ready != null) {
            return ready;
        }

        ProtocolInfo<?> protocol = inboundProtocol;
        if (protocol == null) {
            return null;
        }

        while (true) {
            FoliumTcpFrameCodec.Frame frame =
                FoliumTcpFrameCodec.tryRead(inboundStream);

            if (frame == null) {
                byte[] chunk = transport.poll();

                if (chunk == null) {
                    return null;
                }

                if (decryptCipher != null) {
                    chunk = updateCipher(
                        decryptCipher,
                        chunk,
                        "decrypt"
                    );
                }

                appendInbound(chunk);
                continue;
            }

            inboundStream = frame.remaining();

            byte[] packetPayload =
                FoliumCompressionCodec.decode(
                    frame.payload(),
                    compressionThreshold,
                    validateDecompressed
                );

            Packet<?> packet = FoliumPacketCodec.decode(
                protocol,
                packetPayload
            );

            Packet<?> logical = acceptInboundPacket(
                protocol.bundlerInfo(),
                packet
            );

            if (logical != null) {
                return logical;
            }
        }
    }

    private Packet<?> acceptInboundPacket(
        BundlerInfo bundlerInfo,
        Packet<?> packet
    ) {
        if (inboundBundler != null) {
            verifyNonTerminal(packet);

            Packet<?> bundled = inboundBundler.addPacket(packet);

            if (bundled != null) {
                inboundBundler = null;
                return bundled;
            }

            return null;
        }

        BundlerInfo.Bundler newBundler =
            bundlerInfo.startPacketBundling(packet);

        if (newBundler != null) {
            verifyNonTerminal(packet);
            inboundBundler = newBundler;
            return null;
        }

        return packet;
    }

    private static void verifyNonTerminal(Packet<?> packet) {
        if (packet.isTerminal()) {
            throw new IllegalStateException(
                "Terminal message received inside packet bundle"
            );
        }
    }

    private void appendInbound(byte[] chunk) {
        if (chunk.length == 0) {
            return;
        }

        int oldLength = inboundStream.length;

        inboundStream = Arrays.copyOf(
            inboundStream,
            oldLength + chunk.length
        );

        System.arraycopy(
            chunk,
            0,
            inboundStream,
            oldLength,
            chunk.length
        );
    }

    private static byte[] updateCipher(
        Cipher cipher,
        byte[] input,
        String operation
    ) {
        byte[] output = cipher.update(input);

        if (output == null) {
            throw new IllegalStateException(
                "Minecraft AES/CFB8 " +
                    operation +
                    " produced no output"
            );
        }

        return output;
    }

    @Override
    public void close() {
        transport.close();
    }
}
