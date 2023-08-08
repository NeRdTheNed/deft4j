package com.github.NeRdTheNed.deft4j.util.compression;

import java.util.concurrent.Callable;

class CompressorTask implements Callable<byte[][]> {

    private final Compressor compressor;
    private final byte[] uncompressedData;

    CompressorTask(Compressor comp, byte[] uncompressedData) {
        compressor = comp;
        this.uncompressedData = uncompressedData;
    }

    @Override
    public byte[][] call() throws Exception {
        if (CompressionUtil.PRINT_OPT_FINE) {
            System.out.println("Trying compressor " + compressor.getName());
        }

        return compressor.compress(uncompressedData);
    }

}
