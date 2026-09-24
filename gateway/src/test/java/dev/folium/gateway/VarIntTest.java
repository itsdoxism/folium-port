package dev.folium.gateway;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class VarIntTest {
    @Test
    void roundTripsRepresentativeMinecraftLengths() throws Exception {
        int[] values = {
            0,
            1,
            2,
            127,
            128,
            255,
            2097151,
            8 * 1024 * 1024
        };

        for (int value : values) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            VarInt.write(out, value);

            int decoded = VarInt.read(
                new ByteArrayInputStream(out.toByteArray())
            );

            assertEquals(value, decoded);
        }
    }
}
