package com.github.NeRdTheNed.deft4j.util.compression;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import ru.eustas.zopfli.Options;
import ru.eustas.zopfli.Options.BlockSplitting;
import ru.eustas.zopfli.Options.OutputFormat;
import ru.eustas.zopfli.Zopfli;

/** Compressor using CafeUndZopfli */
public class MultiCafeUndZopfliCompressor implements MultiCompressor<Options> {
    /** Cached compressor */
    private final Zopfli zopfliCompressor;
    /** CafeUndZopfli options */
    private final Options[] options;

    /** Construct CafeUndZopfli options for the given settings */
    private static Options[] getOptions(int iter, boolean extensive) {
        return extensive ? new Options[] {
                   new Options(OutputFormat.DEFLATE, BlockSplitting.FIRST, iter),
                   new Options(OutputFormat.DEFLATE, BlockSplitting.LAST, iter),
                   new Options(OutputFormat.DEFLATE, BlockSplitting.NONE, iter),
               } : new Options[] { new Options(OutputFormat.DEFLATE, BlockSplitting.FIRST, iter) };
    }

    private MultiCafeUndZopfliCompressor(Zopfli zopfliCompressor, Options[] options) {
        this.zopfliCompressor = zopfliCompressor;
        this.options = options;
    }

    private MultiCafeUndZopfliCompressor(Options[] options) {
        this(new Zopfli(8 << 20), options);
    }

    MultiCafeUndZopfliCompressor(int iter, boolean extensive) {
        this(getOptions(iter, extensive));
    }

    /**
     * Compress using Zopfli deflate compression.
     *
     * @param uncompressedData uncompressed data
     * @param options zopfli compression options
     * @return compressed data
     */
    @Override
    public byte[] compressWithOptions(byte[] uncompressedData, Options options) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        zopfliCompressor.compress(options, uncompressedData, bos);
        return bos.toByteArray();
    }

    @Override
    public String getName() {
        return "CafeUndZopfli";
    }

    @Override
    public Options[] getOptions() {
        return options;
    }

}
