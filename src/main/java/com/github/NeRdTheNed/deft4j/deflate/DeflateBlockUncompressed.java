package com.github.NeRdTheNed.deft4j.deflate;

import java.io.IOException;

import com.github.NeRdTheNed.deft4j.io.BitInputStream;
import com.github.NeRdTheNed.deft4j.io.BitOutputStream;
import com.github.NeRdTheNed.deft4j.util.BitInputStreamUtil;

public class DeflateBlockUncompressed extends DeflateBlock {
    byte[] storedData;

    public DeflateBlockUncompressed(DeflateBlock prevBlock) {
        super(prevBlock);
    }

    @Override
    public DeflateBlockType getDeflateBlockType() {
        return DeflateBlockType.STORED;
    }

    @Override
    public boolean parse(BitInputStream is) throws IOException {
        is.readToByteAligned();
        long len, nlen;
        len  = is.readBits(16) & 0xffff;
        nlen = is.readBits(16) & 0xffff;

        if (nlen != (~len & 0xffff)) {
            return false;
        }

        storedData = BitInputStreamUtil.readFromBIS(is, (int) len);
        return true;
    }

    @Override
    public boolean write(BitOutputStream os, boolean finalBlock) throws IOException {
        final int finalBlockI = finalBlock ? 1 : 0;
        os.writeNBits((0x0 << 1) | finalBlockI, 3);
        final byte[] lenNlen = new byte[4];
        lenNlen[0] = (byte)(storedData.length >>> 0);
        lenNlen[1] = (byte)(storedData.length >>> 8);
        lenNlen[2] = (byte) ~lenNlen[0];
        lenNlen[3] = (byte) ~lenNlen[1];
        os.flushToByteAligned();
        os.writeBytes(lenNlen);
        os.writeBytes(storedData);
        return true;
    }

    @Override
    public byte[] getUncompressedData() {
        return storedData;
    }

    @Override
    public boolean fromUncompressed(byte[] input) {
        storedData = input;
        return true;
    }

    @Override
    public long getSizeBits(long alginment) {
        long contributedAlignment = alginment % 8;
        contributedAlignment = contributedAlignment == 0 ? 0 : 8 - contributedAlignment;
        return (((long) storedData.length + 4) * 8) + contributedAlignment;
    }

    @Override
    public long optimise() {
        // TODO Merge blocks? Align?
        return 0;
    }

    @Override
    public DeflateBlock copy() {
        final DeflateBlockUncompressed uncom = new DeflateBlockUncompressed(getPrevious());
        copy(this, uncom);
        //uncom.storedData = new byte[storedData.length];
        //System.arraycopy(storedData, 0, uncom.storedData, 0, storedData.length);
        uncom.storedData = storedData;
        return uncom;
    }

}
