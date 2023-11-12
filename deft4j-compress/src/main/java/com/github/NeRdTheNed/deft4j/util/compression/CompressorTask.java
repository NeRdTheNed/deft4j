package com.github.NeRdTheNed.deft4j.util.compression;

import java.util.concurrent.Callable;

import com.github.NeRdTheNed.deft4j.Deft;

class CompressorTask implements Callable<byte[][]> {

    private final Compressor compressor;
    private final byte[] uncompressedData;
    private final boolean optimiseDeft;
    private final boolean mergeBlocks;

    CompressorTask(Compressor comp, byte[] uncompressedData, boolean optimiseDeft, boolean mergeBlocks) {
        compressor = comp;
        this.uncompressedData = uncompressedData;
        this.optimiseDeft = optimiseDeft;
        this.mergeBlocks = mergeBlocks;
    }

    @Override
    public byte[][] call() throws Exception {
        if (CompressionUtil.PRINT_OPT_FINE) {
            System.out.println("Trying compressor " + compressor.getName());
        }

        final byte[][] compressed = compressor.compress(uncompressedData);

        if (optimiseDeft) {
            final int length = compressed.length;

            for (int i = 0; i < length; i++) {
                compressed[i] = Deft.optimiseDeflateStream(compressed[i], mergeBlocks);
            }
        }

        return compressed;
    }

}
