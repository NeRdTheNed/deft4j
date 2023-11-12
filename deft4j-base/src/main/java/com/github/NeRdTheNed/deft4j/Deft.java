package com.github.NeRdTheNed.deft4j;

import java.io.IOException;

import com.github.NeRdTheNed.deft4j.deflate.DeflateStream;

public class Deft {
    // Debug flags

    // Print optimisation information
    public static final boolean PRINT_OPT = true;
    public static final boolean PRINT_OPT_FINE = PRINT_OPT && false;
    public static final boolean PRINT_OPT_FINER = PRINT_OPT_FINE && false;

    /** Optimise a raw deflate stream */
    public static byte[] optimiseDeflateStream(byte[] original) {
        return optimiseDeflateStream(original, true);
    }

    /** Optimise a raw deflate stream */
    public static byte[] optimiseDeflateStream(byte[] original, boolean mergeBlocks) {
        final DeflateStream stream = new DeflateStream();

        try {
            if (stream.parse(original) && (stream.optimise(mergeBlocks) > 0)) {
                return stream.asBytes();
            }
        } catch (final IOException e) {
            System.err.println("Failed to parse deflate stream data");
            e.printStackTrace();
        }

        return original;
    }

    /** Get the size of a raw deflate stream, in bits. Throws an IOException if it can't be parsed */
    private static long getSizeBits(byte[] deflateStream) throws IOException {
        final DeflateStream stream = new DeflateStream();

        if (!stream.parse(deflateStream)) {
            throw new IOException("Failed to parse deflate stream data");
        }

        return stream.getSizeBits();
    }

    /** Get the size of a raw deflate stream, in bits. If it can't be parsed, round up to the nearest byte of the size of the compressed data. */
    public static long getSizeBitsFallback(byte[] deflateStream) {
        try {
            return getSizeBits(deflateStream);
        } catch (final IOException e) {
            return (long) deflateStream.length * 8;
        }
    }


}
