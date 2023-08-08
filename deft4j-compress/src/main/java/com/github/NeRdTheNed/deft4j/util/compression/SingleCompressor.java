package com.github.NeRdTheNed.deft4j.util.compression;

import java.io.IOException;
import java.util.function.Consumer;

public interface SingleCompressor extends Compressor {
    /** Compress data to a deflate stream */
    byte[] compressSingle(byte[] uncompressedData) throws IOException;

    @Override
    default void compress(byte[] uncompressedData, Consumer<byte[]> callback) throws IOException {
        callback.accept(compressSingle(uncompressedData));
    }

    @Override
    default byte[][] compress(byte[] uncompressedData) throws IOException {
        return new byte[][] { compressSingle(uncompressedData) };
    }
}
