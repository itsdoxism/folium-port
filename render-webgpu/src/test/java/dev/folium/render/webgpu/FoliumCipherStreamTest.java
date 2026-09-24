package dev.folium.render.webgpu;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

final class FoliumCipherStreamTest {
    @Test
    void cfb8StateContinuesAcrossChunks() throws Exception {
        byte[] keyBytes = {
            0, 1, 2, 3, 4, 5, 6, 7,
            8, 9, 10, 11, 12, 13, 14, 15
        };

        SecretKeySpec key = new SecretKeySpec(
            keyBytes,
            "AES"
        );

        IvParameterSpec iv = new IvParameterSpec(keyBytes);

        Cipher encrypt = Cipher.getInstance(
            "AES/CFB8/NoPadding"
        );
        encrypt.init(Cipher.ENCRYPT_MODE, key, iv);

        Cipher decrypt = Cipher.getInstance(
            "AES/CFB8/NoPadding"
        );
        decrypt.init(Cipher.DECRYPT_MODE, key, iv);

        byte[] first = {1, 2, 3, 4, 5};
        byte[] second = {6, 7, 8, 9};

        byte[] encryptedFirst = encrypt.update(first);
        byte[] encryptedSecond = encrypt.update(second);

        assertArrayEquals(
            first,
            decrypt.update(encryptedFirst)
        );
        assertArrayEquals(
            second,
            decrypt.update(encryptedSecond)
        );
    }
}
