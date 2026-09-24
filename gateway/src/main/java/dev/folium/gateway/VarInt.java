package dev.folium.gateway;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class VarInt {
    private VarInt() {
    }

    static int read(InputStream in) throws IOException {
        int value = 0;
        int position = 0;

        while (true) {
            int current = in.read();
            if (current < 0) {
                throw new EOFException("EOF while reading Minecraft VarInt");
            }

            value |= (current & 0x7F) << position;

            if ((current & 0x80) == 0) {
                return value;
            }

            position += 7;
            if (position >= 35) {
                throw new IOException("Minecraft VarInt is too large");
            }
        }
    }

    static void write(OutputStream out, int value) throws IOException {
        int remaining = value;

        while ((remaining & ~0x7F) != 0) {
            out.write((remaining & 0x7F) | 0x80);
            remaining >>>= 7;
        }

        out.write(remaining);
    }
}
