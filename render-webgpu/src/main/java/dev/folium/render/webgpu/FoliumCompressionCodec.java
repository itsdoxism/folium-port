package dev.folium.render.webgpu;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class FoliumCompressionCodec {
    private static final int MAX_PACKET_SIZE = 8 * 1024 * 1024;
    private static final int BUFFER_SIZE = 8192;

    private FoliumCompressionCodec() {
    }

    public static byte[] encode(byte[] packet, int threshold) {
        if (threshold < 0) {
            return packet;
        }

        if (packet.length > MAX_PACKET_SIZE) {
            throw new IllegalArgumentException(
                "Folium packet exceeds Minecraft maximum: " +
                    packet.length
            );
        }

        if (packet.length < threshold) {
            byte[] length = writeVarInt(0);
            byte[] result = new byte[length.length + packet.length];

            System.arraycopy(length, 0, result, 0, length.length);
            System.arraycopy(
                packet,
                0,
                result,
                length.length,
                packet.length
            );

            return result;
        }

        Deflater deflater = new Deflater();

        try {
            deflater.setInput(packet);
            deflater.finish();

            ByteArrayOutputStream compressed =
                new ByteArrayOutputStream(packet.length);

            byte[] buffer = new byte[BUFFER_SIZE];

            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                compressed.write(buffer, 0, count);
            }

            byte[] length = writeVarInt(packet.length);
            byte[] body = compressed.toByteArray();
            byte[] result = new byte[length.length + body.length];

            System.arraycopy(length, 0, result, 0, length.length);
            System.arraycopy(
                body,
                0,
                result,
                length.length,
                body.length
            );

            return result;
        } finally {
            deflater.end();
        }
    }

    public static byte[] decode(
        byte[] envelope,
        int threshold,
        boolean validate
    ) {
        if (threshold < 0) {
            return envelope;
        }

        VarIntResult header = readVarInt(envelope, 0);
        int uncompressedLength = header.value();

        if (uncompressedLength == 0) {
            return Arrays.copyOfRange(
                envelope,
                header.bytesRead(),
                envelope.length
            );
        }

        if (validate && uncompressedLength < threshold) {
            throw new IllegalStateException(
                "Compressed packet is below threshold: " +
                    uncompressedLength +
                    " < " +
                    threshold
            );
        }

        if (uncompressedLength > MAX_PACKET_SIZE) {
            throw new IllegalStateException(
                "Compressed packet expands beyond Minecraft maximum: " +
                    uncompressedLength
            );
        }

        Inflater inflater = new Inflater();

        try {
            inflater.setInput(
                envelope,
                header.bytesRead(),
                envelope.length - header.bytesRead()
            );

            byte[] result = new byte[uncompressedLength];
            int written = 0;

            while (
                written < result.length &&
                !inflater.finished()
            ) {
                int count = inflater.inflate(
                    result,
                    written,
                    result.length - written
                );

                if (count == 0) {
                    if (
                        inflater.needsInput() ||
                        inflater.needsDictionary()
                    ) {
                        break;
                    }
                }

                written += count;
            }

            if (written != uncompressedLength) {
                throw new IllegalStateException(
                    "Minecraft compressed packet expanded to " +
                        written +
                        " bytes, expected " +
                        uncompressedLength
                );
            }

            return result;
        } catch (DataFormatException error) {
            throw new IllegalStateException(
                "Invalid Minecraft zlib packet",
                error
            );
        } finally {
            inflater.end();
        }
    }

    static byte[] writeVarInt(int value) {
        byte[] result = new byte[5];
        int index = 0;
        int remaining = value;

        while ((remaining & ~0x7F) != 0) {
            result[index++] = (byte) (
                (remaining & 0x7F) | 0x80
            );
            remaining >>>= 7;
        }

        result[index++] = (byte) remaining;
        return Arrays.copyOf(result, index);
    }

    static VarIntResult readVarInt(byte[] input, int offset) {
        int value = 0;
        int position = 0;
        int index = offset;

        while (index < input.length) {
            int current = input[index++] & 0xFF;
            value |= (current & 0x7F) << position;

            if ((current & 0x80) == 0) {
                return new VarIntResult(
                    value,
                    index - offset
                );
            }

            position += 7;
            if (position >= 35) {
                throw new IllegalStateException(
                    "Minecraft VarInt is too large"
                );
            }
        }

        throw new IllegalStateException(
            "EOF while reading Minecraft VarInt"
        );
    }

    record VarIntResult(int value, int bytesRead) {
    }
}
