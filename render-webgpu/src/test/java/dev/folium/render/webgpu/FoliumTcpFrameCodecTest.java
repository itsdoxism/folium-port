package dev.folium.render.webgpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class FoliumTcpFrameCodecTest {
    @Test
    void waitsForCompleteFrame() {
        byte[] payload = new byte[300];
        byte[] framed = FoliumTcpFrameCodec.frame(payload);

        byte[] partial = new byte[2];
        System.arraycopy(framed, 0, partial, 0, partial.length);

        assertNull(FoliumTcpFrameCodec.tryRead(partial));
    }

    @Test
    void readsFrameAndPreservesRemainingBytes() {
        byte[] first = {1, 2, 3};
        byte[] second = {4, 5};

        byte[] a = FoliumTcpFrameCodec.frame(first);
        byte[] b = FoliumTcpFrameCodec.frame(second);
        byte[] joined = new byte[a.length + b.length];

        System.arraycopy(a, 0, joined, 0, a.length);
        System.arraycopy(b, 0, joined, a.length, b.length);

        FoliumTcpFrameCodec.Frame frame =
            FoliumTcpFrameCodec.tryRead(joined);

        assertArrayEquals(first, frame.payload());
        assertArrayEquals(b, frame.remaining());
    }
}
