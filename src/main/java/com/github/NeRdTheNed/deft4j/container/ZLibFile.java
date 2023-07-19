package com.github.NeRdTheNed.deft4j.container;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Adler32;

import com.github.NeRdTheNed.deft4j.deflate.DeflateStream;
import com.github.NeRdTheNed.deft4j.util.Util;

public class ZLibFile implements DeflateFilesContainer {
    DeflateStream deflateStream;

    private int CMF;
    private int FLG;

    long adler32;

    @Override
    public List<DeflateStream> getDeflateStreams() {
        final ArrayList<DeflateStream> fileList = new ArrayList<>();
        fileList.add(deflateStream);
        return fileList;
    }

    /** Cached Adler32 calculator */
    private final Adler32 adler32Calc = new Adler32();

    @Override
    public boolean write(OutputStream os) throws IOException {
        os.write(CMF);
        os.write(FLG);

        if (!deflateStream.write(os)) {
            return false;
        }

        if (RECALC) {
            final byte[] uncompressedData = deflateStream.getUncompressedData();
            adler32Calc.reset();
            adler32Calc.update(uncompressedData);
            final long realAdler32 = Util.revByteOrder32(adler32Calc.getValue());

            if (realAdler32 != adler32) {
                System.err.println("Warning: calculated Alder32 " + Util.revByteOrder32(realAdler32) + " did not match expected Alder32 " + Util.revByteOrder32(adler32));
            }

            Util.writeIntLE(os, (int) realAdler32);
        } else {
            Util.writeIntLE(os, (int) adler32);
        }

        return true;
    }

    @Override
    public boolean read(InputStream is) throws IOException {
        CMF = is.read();

        // Check compression method, only deflate compression is supported
        if ((CMF & 0xF) != 8) {
            System.err.println("ZLib non-deflate compression method " + (CMF & 0xF) + " not supported");
            return false;
        }

        FLG = is.read();

        // RFC 1950:
        // The FCHECK value must be such that CMF and FLG, when viewed as
        // a 16-bit unsigned integer stored in MSB order (CMF*256 + FLG),
        // is a multiple of 31.
        if ((((CMF << 8) + FLG) % 31) != 0) {
            System.err.println("ZLib header check failed (FCHECK)");
            return false;
        }

        // Check for preset dictionary (currently not supported)
        // TODO Investigate supporting this
        if ((FLG & 0x20) == 0x20) {
            System.err.println("ZLib preset dictionary currently not supported");
            return false;
        }

        deflateStream = new DeflateStream();

        if (!deflateStream.parse(is)) {
            return false;
        }

        adler32 = (is.read() & 0xff) + ((is.read() & 0xff) << 8) + ((is.read() & 0xff) << 16) + ((long)(is.read() & 0xff) << 24);
        return true;
    }

    @Override
    public String fileType() {
        return "ZLib";
    }

}
