package dev.folium.render.webgpu;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;

public final class FoliumPacketCodec {
    private FoliumPacketCodec() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static byte[] encode(
        ProtocolInfo<?> protocol,
        Packet<?> packet
    ) {
        ByteBuf buffer = Unpooled.buffer();

        try {
            ((ProtocolInfo) protocol)
                .codec()
                .encode(buffer, packet);

            byte[] payload = new byte[buffer.readableBytes()];
            buffer.getBytes(buffer.readerIndex(), payload);
            return payload;
        } finally {
            buffer.release();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Packet<?> decode(
        ProtocolInfo<?> protocol,
        byte[] payload
    ) {
        ByteBuf buffer = Unpooled.wrappedBuffer(payload);

        try {
            Packet<?> packet = (Packet<?>) ((ProtocolInfo) protocol)
                .codec()
                .decode(buffer);

            if (buffer.isReadable()) {
                throw new IllegalStateException(
                    "Folium packet decoder left " +
                        buffer.readableBytes() +
                        " unread bytes"
                );
            }

            return packet;
        } finally {
            buffer.release();
        }
    }
}
