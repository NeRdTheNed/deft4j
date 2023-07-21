package com.github.NeRdTheNed.deft4j.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class BitInputStream implements Closeable {
    private static final int ALIGN = 8;

    private final InputStream is;

    private long pos;

    private long accum;
    private int bitpos = 0;

    private boolean eof = false;

    public BitInputStream(InputStream is) {
        this.is = is;
    }

    private long read8AssumeAligned() throws IOException {
        if (eof) {
            return -1;
        }

        pos++;
        final long toReturn = is.read();

        if (toReturn == -1) {
            eof = true;
            return -1;
        }

        return toReturn;
    }

    public long readBit() throws IOException {
        if (eof) {
            return -1;
        }

        if (bitpos == 0) {
            bitpos = ALIGN;
            accum = read8AssumeAligned();

            if (eof) {
                return -1;
            }
        }

        final long returnVal = accum & 1;
        accum >>>= 1;
        bitpos--;
        return returnVal;
    }

    public long readBits(int count) throws IOException {
        if (eof) {
            return -1;
        }

        long read = 0;
        long readAmount = 0;

        while (count > 0) {
            final boolean readAlgined = (count >= ALIGN) && (bitpos == 0);
            final long thisRead = readAlgined ? read8AssumeAligned() : readBit();
            final long thisReadAmount = readAlgined ? ALIGN : 1;

            if (eof) {
                return -1;
            }

            read |= (thisRead << readAmount);
            readAmount += thisReadAmount;
            count -= thisReadAmount;
        }

        return read;
    }

    public void readToByteAligned() throws IOException {
        if (bitpos != 0) {
            readBits(bitpos);
        }
    }

    public long getBytesRead() {
        return pos;
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

}
