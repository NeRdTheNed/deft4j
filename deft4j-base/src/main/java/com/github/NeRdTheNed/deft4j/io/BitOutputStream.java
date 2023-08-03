package com.github.NeRdTheNed.deft4j.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

public class BitOutputStream implements Closeable {
    private static final int ALIGN = 8;

    private final OutputStream os;

    private long pos;

    private int accum;
    private int bitpos = ALIGN;

    public BitOutputStream(OutputStream os) {
        this.os = os;
    }

    private void writeByteAssumeAligned(int b) throws IOException {
        os.write(b);
        pos++;
    }

    private void writeBytesAssumeAligned(byte[] bytes) throws IOException {
        os.write(bytes);
        pos += bytes.length;
    }

    private void writeBit(int bit) throws IOException {
        accum |= (bit & 1) << (ALIGN - 1 - --bitpos);

        if (bitpos == 0) {
            writeByteAssumeAligned(accum);
            bitpos = ALIGN;
            accum = 0;
        }
    }

    public void writeNBits(long bits, int n) throws IOException {
        for (int i = 0; i < n; i++) {
            writeBit((int) ((bits >>> i) & 1));
        }
    }

    private void writeByte(int b) throws IOException {
        if (bitpos != ALIGN) {
            writeNBits(b & 0xFF, 8);
        } else {
            writeByteAssumeAligned(b);
        }
    }

    public void writeBytes(byte[] bytes) throws IOException {
        if (bitpos != ALIGN) {
            for (final byte b : bytes) {
                writeByte(b & 0xFF);
            }
        } else {
            writeBytesAssumeAligned(bytes);
        }
    }

    public void flushToByteAligned() throws IOException {
        if (bitpos != ALIGN) {
            writeNBits(0, bitpos);
        }
    }

    public long getBytesWritten() {
        return pos;
    }

    public int getRemainingBits() {
        return bitpos;
    }

    @Override
    public void close() throws IOException {
        flushToByteAligned();
        os.close();
    }

}
