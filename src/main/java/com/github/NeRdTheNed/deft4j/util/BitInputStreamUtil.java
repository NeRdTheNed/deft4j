package com.github.NeRdTheNed.deft4j.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.github.NeRdTheNed.deft4j.io.BitInputStream;

public class BitInputStreamUtil {
    /** Private constructor to hide the default one */
    private BitInputStreamUtil() {
        // This space left intentionally blank
    }

    public static byte[] readFromBIS(BitInputStream is, int length) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (int i = 0; i < length; i++) {
            baos.write((byte) is.readBits(8));
        }

        return baos.toByteArray();
    }
}
