package com.github.NeRdTheNed.deft4j.util.compression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

interface Compressor {
    /** Compress data to a deflate stream, sending each compressed result to the given consumer */
    void compress(byte[] uncompressedData, Consumer<byte[]> callback) throws IOException;

    default byte[][] compress(byte[] uncompressedData) throws IOException {
        final List<byte[]> compressedResults = new ArrayList<>();
        compress(uncompressedData, compressedResults::add);
        return compressedResults.toArray(new byte[0][]);
    }

    String getName();
}
