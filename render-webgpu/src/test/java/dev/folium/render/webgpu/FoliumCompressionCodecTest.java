package dev.folium.render.webgpu;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

final class FoliumCompressionCodecTest {
    @Test
    void roundTripsRawPacketBelowThreshold() {
        byte[] packet = "small packet".getBytes(
            StandardCharsets.UTF_8
        );

        byte[] encoded = FoliumCompressionCodec.encode(
            packet,
            256
        );

        assertArrayEquals(
            packet,
            FoliumCompressionCodec.decode(
                encoded,
                256,
                true
            )
        );
    }

    @Test
    void roundTripsCompressedPacketAtThreshold() {
        byte[] packet = new byte[4096];
        Arrays.fill(packet, (byte) 42);

        byte[] encoded = FoliumCompressionCodec.encode(
            packet,
            256
        );

        assertArrayEquals(
            packet,
            FoliumCompressionCodec.decode(
                encoded,
                256,
                true
            )
        );
    }

    @Test
    void disabledCompressionIsIdentity() {
        byte[] packet = {1, 2, 3, 4, 5};

        assertArrayEquals(
            packet,
            FoliumCompressionCodec.encode(packet, -1)
        );

        assertArrayEquals(
            packet,
            FoliumCompressionCodec.decode(
                packet,
                -1,
                true
            )
        );
    }
}
