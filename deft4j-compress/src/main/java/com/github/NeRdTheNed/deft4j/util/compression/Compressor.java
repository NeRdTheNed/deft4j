package com.github.NeRdTheNed.deft4j.util.compression;

import java.io.IOException;

interface Compressor {
    /** Compress data to a deflate stream */
    byte[] compress(byte[] uncompressedData) throws IOException;
    String getName();
}
