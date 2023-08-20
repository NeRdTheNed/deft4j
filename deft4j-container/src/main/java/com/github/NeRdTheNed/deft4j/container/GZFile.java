package com.github.NeRdTheNed.deft4j.container;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import com.github.NeRdTheNed.deft4j.deflate.DeflateStream;
import com.github.NeRdTheNed.deft4j.util.Util;

public class GZFile implements DeflateFilesContainer {
    // private static final int FTEXT    = 0b00001;
    private static final int FHCRC    = 0b00010;
    private static final int FEXTRA   = 0b00100;
    private static final int FNAME    = 0b01000;
    private static final int FCOMMENT = 0b10000;

    static class GZipExtraFormat {
        int xlength;
        byte[] data;
    }

    private int compressionMethod;
    private int flags;
    private long time;
    private int extraFlags;
    private int os = 255;

    private GZipExtraFormat extra;
    private String filename;
    private String comment;
    private int crc16;

    private DeflateStream deflateStream;

    private long crc32;
    private long isize;

    @Override
    public boolean read(InputStream is) throws IOException {
        // Check if data is likely GZip
        if ((is == null)
                // Check GZip signature
                || (is.read() != 0x1f) || (is.read() != 0x8b)
                // Check compression method for deflate
                || ((compressionMethod = is.read()) != 8)
                // Check for reserved flags
                || (((flags = is.read()) & 0xe0) != 0)) {
            return false;
        }

        time = is.read() + (is.read() << 8) + (is.read() << 16) + ((long)is.read() << 24);
        extraFlags = is.read();
        os = is.read();

        if ((flags & FEXTRA) != 0) {
            final int xlen = is.read() + (is.read() << 8);
            extra = new GZipExtraFormat();
            extra.xlength = xlen;
            extra.data = new byte[xlen];
            is.read(extra.data, 0, xlen);
        }

        if ((flags & FNAME) != 0) {
            setFilename(Util.readStr(is));
        }

        if ((flags & FCOMMENT) != 0) {
            comment = Util.readStr(is);
        }

        if ((flags & FHCRC) != 0) {
            crc16 = is.read() + (is.read() << 8);
        }

        deflateStream = new DeflateStream(filename);

        if (!deflateStream.parse(is)) {
            return false;
        }

        crc32 = (is.read() & 0xff) + ((is.read() & 0xff) << 8) + ((is.read() & 0xff) << 16) + ((long)(is.read() & 0xff) << 24);
        isize = (is.read() & 0xff) + ((is.read() & 0xff) << 8) + ((is.read() & 0xff) << 16) + ((long)(is.read() & 0xff) << 24);
        return true;
    }

    /** Cached CRC32 calculator */
    private final CRC32 crc32Calc = new CRC32();

    @Override
    public boolean write(OutputStream os) throws IOException {
        if (os == null) {
            return false;
        }

        // Header
        os.write(0x1f);
        os.write(0x8b);
        os.write(compressionMethod);
        os.write(flags);
        Util.writeIntLE(os, (int) time);
        os.write(extraFlags);
        os.write(this.os);

        if ((flags & FEXTRA) != 0) {
            Util.writeShortLE(os, extra.xlength);
            os.write(extra.data);
        }

        if ((flags & FNAME) != 0) {
            os.write(filename.getBytes());
            os.write(0);
        }

        if ((flags & FCOMMENT) != 0) {
            os.write(comment.getBytes());
        }

        if ((flags & FHCRC) != 0) {
            Util.writeShortLE(os, crc16);
        }

        if (!deflateStream.write(os)) {
            return false;
        }

        if (RECALC) {
            final byte[] uncompressedData = deflateStream.getUncompressedData();
            crc32Calc.reset();
            crc32Calc.update(uncompressedData);
            final long realCRC32 = crc32Calc.getValue();
            final int realISize = uncompressedData.length;

            if (realCRC32 != crc32) {
                System.err.println("Warning: calculated CRC32 " + realCRC32 + " did not match expected CRC32 " + crc32);
            }

            if (isize != realISize) {
                System.err.println("Warning: calculated size " + realISize + " did not match expected size " + isize);
            }

            Util.writeIntLE(os, (int) realCRC32);
            Util.writeIntLE(os, realISize);
        } else {
            Util.writeIntLE(os, (int) crc32);
            Util.writeIntLE(os, (int) isize);
        }

        return true;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
        final boolean alreadyHasName = (flags & FNAME) != 0;

        if ((filename != null) && !filename.isEmpty()) {
            if (!alreadyHasName) {
                flags |= FNAME;
            }
        } else if (alreadyHasName) {
            flags &= ~FNAME;
        }
    }

    public void setData(DeflateStream stream) throws IOException {
        deflateStream = stream;
        compressionMethod = 8;
        final byte[] uncompressedData = deflateStream.getUncompressedData();
        crc32Calc.reset();
        crc32Calc.update(uncompressedData);
        crc32 = crc32Calc.getValue();
        isize = uncompressedData.length;
    }

    @Override
    public List<DeflateStream> getDeflateStreams() {
        final List<DeflateStream> fileList = new ArrayList<>();
        fileList.add(deflateStream);
        return fileList;
    }

    @Override
    public String fileType() {
        return "GZip";
    }
}
