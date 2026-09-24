package dev.folium.render.webgpu;

import java.util.Arrays;

public final class FoliumTcpFrameCodec {
    private FoliumTcpFrameCodec() {
    }

    public static byte[] frame(byte[] payload) {
        byte[] length = FoliumCompressionCodec.writeVarInt(
            payload.length
        );

        byte[] result = new byte[length.length + payload.length];

        System.arraycopy(length, 0, result, 0, length.length);
        System.arraycopy(
            payload,
            0,
            result,
            length.length,
            payload.length
        );

        return result;
    }

    public static Frame tryRead(byte[] buffer) {
        int value = 0;
        int position = 0;

        for (int index = 0; index < buffer.length && index < 5; index++) {
            int current = buffer[index] & 0xFF;
            value |= (current & 0x7F) << position;

            if ((current & 0x80) == 0) {
                int headerLength = index + 1;

                if (value < 0 || value > 8 * 1024 * 1024) {
                    throw new IllegalStateException(
                        "Invalid Minecraft frame length: " + value
                    );
                }

                if (buffer.length < headerLength + value) {
                    return null;
                }

                byte[] payload = Arrays.copyOfRange(
                    buffer,
                    headerLength,
                    headerLength + value
                );

                byte[] remaining = Arrays.copyOfRange(
                    buffer,
                    headerLength + value,
                    buffer.length
                );

                return new Frame(payload, remaining);
            }

            position += 7;
        }

        if (buffer.length >= 5) {
            throw new IllegalStateException(
                "Minecraft outer frame VarInt is too large"
            );
        }

        return null;
    }

    public record Frame(
        byte[] payload,
        byte[] remaining
    ) {
    }
}
