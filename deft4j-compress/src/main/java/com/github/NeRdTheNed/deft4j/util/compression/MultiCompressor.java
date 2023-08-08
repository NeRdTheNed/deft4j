package com.github.NeRdTheNed.deft4j.util.compression;

import java.io.IOException;
import java.util.function.Consumer;

interface MultiCompressor<T> extends Compressor {
    /** Compress data to a deflate stream with the given settings */
    byte[] compressWithOptions(byte[] uncompressedData, T options) throws IOException;

    T[] getOptions();

    @Override
    default void compress(byte[] uncompressedData, Consumer<byte[]> callback) throws IOException {
        for (final T option : getOptions()) {
            callback.accept(compressWithOptions(uncompressedData, option));
        }
    }

    @Deprecated
    default byte[] compressBasic(byte[] uncompressedData) throws IOException {
        byte[] compressedData = null;

        for (final T option : getOptions()) {
            final byte[] currentResult = compressWithOptions(uncompressedData, option);

            if ((compressedData == null) || (currentResult.length < compressedData.length)) {
                compressedData = currentResult;
            }
        }

        if (compressedData == null) {
            throw new IOException("Unable to compress data with " + getName());
        }

        return compressedData;
    }
}
