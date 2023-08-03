package com.github.NeRdTheNed.deft4j.util.compression;

import java.io.IOException;

interface MultiCompressor<T> extends Compressor {
    /** Compress data to a deflate stream with the given settings */
    byte[] compressWithOptions(byte[] uncompressedData, T options) throws IOException;
}
