package com.github.NeRdTheNed.deft4j.container;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import com.github.NeRdTheNed.deft4j.deflate.DeflateStream;
import com.github.NeRdTheNed.deft4j.util.Util;

public class PNGFile implements DeflateFilesContainer {
    public class PNGChunk {
        byte[] type = new byte[4];
        byte[] data;

        public boolean isIDAT() {
            return (type[0] == 'I') && (type[1] == 'D') && (type[2] == 'A') && (type[3] == 'T');
        }

        public boolean isIEND() {
            return (type[0] == 'I') && (type[1] == 'E') && (type[2] == 'N') && (type[3] == 'D');
        }

        public boolean write(OutputStream os) throws IOException {
            Util.writeIntBE(os, data.length);
            os.write(type);

            if (data.length > 0) {
                os.write(data);
            }

            crc32Calc.reset();
            crc32Calc.update(type);

            if (data.length > 0) {
                crc32Calc.update(data);
            }

            final long CRC32 = crc32Calc.getValue();
            Util.writeIntBE(os, (int) CRC32);
            return true;
        }

        private static final boolean PRINT_CHUNK_INFO = false;

        public boolean read(InputStream is) throws IOException {
            if (PRINT_CHUNK_INFO) {
                System.out.println("PNG chunk:");
            }

            final long length = ((long)(is.read() & 0xff) << 24) + ((is.read() & 0xff) << 16) + ((is.read() & 0xff) << 8) + (is.read() & 0xff);

            if (PRINT_CHUNK_INFO) {
                System.out.println("Chunk length " + length);
            }

            if (length > Integer.MAX_VALUE) {
                return false;
            }

            type[0] = (byte) (is.read() & 0xff);
            type[1] = (byte) (is.read() & 0xff);
            type[2] = (byte) (is.read() & 0xff);
            type[3] = (byte) (is.read() & 0xff);

            if (PRINT_CHUNK_INFO) {
                System.out.println("Chunk type " + new String(type));
            }

            if (length > 0) {
                data = Util.readFromInputStream(is, length);
            } else {
                data = new byte[] { };
            }

            final long CRC32 = ((long)(is.read() & 0xff) << 24) + ((is.read() & 0xff) << 16) + ((is.read() & 0xff) << 8) + (is.read() & 0xff);
            crc32Calc.reset();
            crc32Calc.update(type);

            if (length > 0) {
                crc32Calc.update(data);
            }

            final long calcCrc = crc32Calc.getValue();

            if (PRINT_CHUNK_INFO) {
                System.out.println("Read CRC32 " + CRC32 + ", calculated CRC32 " + calcCrc);
            }

            return calcCrc == CRC32;
        }
    }

    private List<PNGChunk> pngChunks;

    /** Cached CRC32 calculator */
    private final CRC32 crc32Calc = new CRC32();

    private void syncStreams() throws IOException {
        // Find the first IDAT chunk, and store the index
        final PNGChunk firstIDAT = pngChunks.stream().filter(PNGChunk::isIDAT).findFirst().orElseThrow(() -> new IOException("No IDAT chunk found in PNG"));
        int index = pngChunks.indexOf(firstIDAT);
        // Remove all existing IDAT chunks
        pngChunks.removeIf(PNGChunk::isIDAT);
        // Write the optimized ZLIB container to bytes
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (!idat.write(baos)) {
            throw new IOException("Could not write IDAT to bytes");
        }

        final byte[] idatBytes = baos.toByteArray();
        long toWrite = idatBytes.length;

        // Write IDAT chunks, only splitting if the max chunk size is reached
        do {
            final PNGChunk chunk = new PNGChunk();
            // IDAT chunk type
            chunk.type[0] = 'I';
            chunk.type[1] = 'D';
            chunk.type[2] = 'A';
            chunk.type[3] = 'T';
            // Copy as many bytes as the ZLib container has, or the maximum chunk size
            final int toCopy = (int) Math.min(toWrite, Integer.MAX_VALUE);
            chunk.data = new byte[toCopy];
            System.arraycopy(idatBytes, (int) (toWrite - idatBytes.length), chunk.data, 0, toCopy);
            pngChunks.add(index, chunk);
            index++;
            toWrite -= toCopy;
        } while (toWrite > 0);
    }

    // TODO Support other deflate compressed chunks (zTXt ect)
    ZLibFile idat;

    @Override
    public List<DeflateStream> getDeflateStreams() {
        return idat.getDeflateStreams();
    }

    @Override
    public boolean write(OutputStream os) throws IOException {
        syncStreams();
        // PNG header
        os.write(137);
        os.write('P');
        os.write('N');
        os.write('G');
        os.write(13);
        os.write(10);
        os.write(26);
        os.write(10);

        for (final PNGChunk pngChunk : pngChunks) {
            if (!pngChunk.write(os)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean read(InputStream is) throws IOException {
        // Check if data is likely PNG
        if ((is == null)
                // Check PNG signature
                || (is.read() != 137) || (is.read() != 'P') || (is.read() != 'N') || (is.read() != 'G') || (is.read() != 13) || (is.read() != 10) || (is.read() != 26) || (is.read() != 10)) {
            return false;
        }

        // Read all IDAT chunks to bytes
        final ByteArrayOutputStream boas = new ByteArrayOutputStream();
        pngChunks = new ArrayList<>();
        PNGChunk lastChunk;

        do {
            final PNGChunk nextChunk = new PNGChunk();

            if (!nextChunk.read(is)) {
                return false;
            }

            if (nextChunk.isIDAT() && (nextChunk.data.length > 0)) {
                boas.write(nextChunk.data);
            }

            pngChunks.add(nextChunk);
            lastChunk = nextChunk;
        } while (!lastChunk.isIEND());

        // Read the ZLib container from the IDAT bytes
        idat = new ZLibFile();
        return idat.read(boas.toByteArray());
    }

    @Override
    public String fileType() {
        return "PNG";
    }

}
