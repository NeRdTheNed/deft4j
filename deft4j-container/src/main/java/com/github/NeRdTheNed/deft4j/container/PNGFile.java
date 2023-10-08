package com.github.NeRdTheNed.deft4j.container;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.CRC32;

import com.github.NeRdTheNed.deft4j.deflate.DeflateStream;
import com.github.NeRdTheNed.deft4j.util.Util;

public class PNGFile implements DeflateFilesContainer, ToGZipConvertible {
    class PNGChunk {
        final byte[] type = new byte[4];
        byte[] data;
        private long seqNum;

        boolean isIDAT() {
            return (type[0] == 'I') && (type[1] == 'D') && (type[2] == 'A') && (type[3] == 'T');
        }

        boolean isIEND() {
            return (type[0] == 'I') && (type[1] == 'E') && (type[2] == 'N') && (type[3] == 'D');
        }

        boolean iszTXt() {
            return (type[0] == 'z') && (type[1] == 'T') && (type[2] == 'X') && (type[3] == 't');
        }

        boolean isiCCP() {
            return (type[0] == 'i') && (type[1] == 'C') && (type[2] == 'C') && (type[3] == 'P');
        }

        boolean isiTXt() {
            return (type[0] == 'i') && (type[1] == 'T') && (type[2] == 'X') && (type[3] == 't');
        }

        // APNG

        boolean isacTL() {
            return (type[0] == 'a') && (type[1] == 'c') && (type[2] == 'T') && (type[3] == 'L');
        }

        boolean isfcTL() {
            return (type[0] == 'f') && (type[1] == 'c') && (type[2] == 'T') && (type[3] == 'L');
        }

        boolean isfdAT() {
            return (type[0] == 'f') && (type[1] == 'd') && (type[2] == 'A') && (type[3] == 'T');
        }

        boolean hasSeq() {
            return isfdAT() || isfcTL();
        }

        long getSeq() {
            return seqNum;
        }

        void setSeq(long seqNum) {
            this.seqNum = seqNum;
            data[0] = (byte) ((seqNum >>> 24) & 0xff);
            data[1] = (byte) ((seqNum >>> 16) & 0xff);
            data[2] = (byte) ((seqNum >>> 8) & 0xff);
            data[3] = (byte) (seqNum & 0xff);
        }

        boolean isZLibCompressedNonIdat() {
            return iszTXt() || isiCCP() || isiTXt();
        }

        String type() {
            return new String(type);
        }

        byte[] getZLibCompressedNonIdat() {
            final boolean isiTXt = isiTXt();

            if (isiTXt || iszTXt() || isiCCP()) {
                int offset = Util.strlen(data, 0) + 2;

                if (isiTXt) {
                    if (data[offset - 1] != 1) {
                        return null;
                    }

                    offset++;
                }

                if (data[offset - 1] != 0) {
                    System.out.println("Only deflate compression is currently supported for " + type() + " chunks (read method " + data[offset - 1] + ")");
                    return null;
                }

                if (isiTXt) {
                    offset += Util.strlen(data, offset) + 1;
                    offset += Util.strlen(data, offset) + 1;
                }

                final int length = data.length - offset;
                final byte[] ret = new byte[length];
                System.arraycopy(data, offset, ret, 0, length);
                return ret;
            }

            assert !isZLibCompressedNonIdat();
            return null;
        }

        void setZLibCompressedNonIdat(byte[] newZLibData) {
            final boolean isiTXt = isiTXt();

            if (isiTXt || iszTXt() || isiCCP()) {
                int offset = Util.strlen(data, 0) + 2;

                if (isiTXt) {
                    offset++;
                    offset += Util.strlen(data, offset) + 1;
                    offset += Util.strlen(data, offset) + 1;
                }

                final int length = newZLibData.length + offset;
                final byte[] newData = new byte[length];
                System.arraycopy(data, 0, newData, 0, offset);
                System.arraycopy(newZLibData, 0, newData, offset, newZLibData.length);
                data = newData;
                return;
            }

            assert !isZLibCompressedNonIdat();
        }

        boolean write(OutputStream os) throws IOException {
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

        boolean read(InputStream is) throws IOException {
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

            if (hasSeq()) {
                seqNum = ((long)(data[0] & 0xff) << 24) + ((data[1] & 0xff) << 16) + ((data[2] & 0xff) << 8) + (data[3] & 0xff);

                if (PRINT_CHUNK_INFO) {
                    System.out.println("Sequence number " + seqNum);
                }
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

    @Override
    public List<GZFile> asGZipFiles() throws IOException {
        final List<GZFile> converted = new ArrayList<>();
        final GZFile idatGZ = new GZFile();
        idatGZ.setData(idat.deflateStream);
        idatGZ.setFilename("IDAT");
        converted.add(idatGZ);
        final Map<String, Integer> seenChunks = new HashMap<>();

        for (final Entry<PNGChunk, DeflateFilesContainer> entry : deflateStreamMapNonIDAT.entrySet()) {
            final PNGChunk chunk = entry.getKey();
            final DeflateFilesContainer container = entry.getValue();
            final String type = chunk.type();
            final int seen = seenChunks.merge(type, 1, Integer::sum);
            final List<DeflateStream> streams = container.getDeflateStreams();
            final int streamsSize = streams.size();

            for (int i = 0; i < streamsSize; i++) {
                final String name = type + "-num-" + seen + "-stream-" + i;
                final GZFile chunkGZ = new GZFile();
                chunkGZ.setData(streams.get(i));
                chunkGZ.setFilename(name);
                converted.add(chunkGZ);
            }
        }

        return converted;
    }

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

        if (fdats != null) {
            final ListIterator<PNGChunk> chunkIter = pngChunks.listIterator();

            for (final ZLibFile fdat : fdats) {
                int fdATIndex = -1;

                while (chunkIter.hasNext()) {
                    final PNGChunk chunk = chunkIter.next();

                    if (fdATIndex == -1) {
                        if (chunk.isfdAT()) {
                            fdATIndex = chunkIter.previousIndex();
                            chunkIter.remove();
                            final ByteArrayOutputStream fbaos = new ByteArrayOutputStream();

                            if (!fdat.write(fbaos)) {
                                throw new IOException("Could not write fdAT to bytes");
                            }

                            final byte[] fdatBytes = fbaos.toByteArray();
                            long ftoWrite = fdatBytes.length;

                            // Write fdAT chunks, only splitting if the max chunk size is reached
                            do {
                                final PNGChunk newChunk = new PNGChunk();
                                // fdAT chunk type
                                newChunk.type[0] = 'f';
                                newChunk.type[1] = 'd';
                                newChunk.type[2] = 'A';
                                newChunk.type[3] = 'T';
                                // Copy as many bytes as the ZLib container has, or the maximum chunk size - 4
                                final int toCopy = (int) Math.min(ftoWrite, Integer.MAX_VALUE - 4L);
                                newChunk.data = new byte[toCopy + 4];
                                System.arraycopy(fdatBytes, (int) (ftoWrite - fdatBytes.length), newChunk.data, 4, toCopy);
                                chunkIter.add(newChunk);
                                ftoWrite -= toCopy;
                            } while (ftoWrite > 0);
                        }
                    } else {
                        // Remove split fdAT chunks
                        if (chunk.isfdAT()) {
                            chunkIter.remove();
                        }

                        // If we've reached the next fcTL chunk, break
                        if (chunk.isfcTL()) {
                            break;
                        }
                    }
                }

                if (fdATIndex == -1) {
                    throw new IOException("Incorrect chunk order in APNG");
                }
            }

            int seq = 0;

            for (final PNGChunk chunk : pngChunks) {
                if (chunk.hasSeq()) {
                    chunk.setSeq(seq);
                    seq++;
                }
            }
        }

        deflateStreamMapNonIDAT.forEach((c, d) -> {
            try {
                c.setZLibCompressedNonIdat(d.write());
            } catch (final IOException e) {
                System.out.println("Error writing deflate stream to " + c.type() + " chunk in PNG file");
                e.printStackTrace();
            }
        });
    }

    private ZLibFile idat;
    private List<ZLibFile> fdats;

    private Map<PNGChunk, DeflateFilesContainer> deflateStreamMapNonIDAT;

    @Override
    public List<DeflateStream> getDeflateStreams() {
        final ArrayList<DeflateStream> fileList = new ArrayList<>(idat.getDeflateStreams());

        if (fdats != null) {
            fdats.forEach(e -> fileList.addAll(e.getDeflateStreams()));
        }

        if (deflateStreamMapNonIDAT != null) {
            deflateStreamMapNonIDAT.values().forEach(e -> fileList.addAll(e.getDeflateStreams()));
        }

        return fileList;
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

    private static final class PNGChunkHelper {
        public ZLibFile helperIdat;
        public List<ZLibFile> helperFdats = null;
        public Map<PNGChunk, DeflateFilesContainer> helperDeflateStreamMapNonIDAT = new HashMap<>();

        private boolean outOfOrder = false;
        private boolean readingfdAT = false;
        private boolean readingIDAT = false;
        private boolean seenacTL = false;
        private boolean seenIDAT = false;
        private boolean seenIEND = false;

        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        private int seq = 0;

        private boolean shouldFlush(PNGChunk chunk) {
            return (readingIDAT && !chunk.isIDAT()) || (readingfdAT && (chunk.isfcTL() || chunk.isIEND()));
        }

        private boolean flush() throws IOException {
            if (readingIDAT) {
                helperIdat = new ZLibFile();

                if (!helperIdat.read(baos.toByteArray())) {
                    return false;
                }

                helperIdat.deflateStream.setName("IDAT chunk");
                seenIDAT = true;
                readingIDAT = false;
            } else {
                assert readingfdAT;

                if (helperFdats == null) {
                    helperFdats = new ArrayList<>();
                }

                final ZLibFile fdat = new ZLibFile();
                helperFdats.add(fdat);

                if (!fdat.read(baos.toByteArray())) {
                    return false;
                }

                fdat.deflateStream.setName("fdAT chunk " + helperFdats.size());
                readingfdAT = false;
            }

            baos.reset();
            return true;
        }

        private boolean setReadState(PNGChunk chunk) {
            if (chunk.isfdAT()) {
                if (!seenIDAT || readingIDAT) {
                    return false;
                }

                readingfdAT = true;
            } else if (chunk.isIDAT()) {
                if (seenIDAT || readingfdAT) {
                    return false;
                }

                readingIDAT = true;
            }

            return true;
        }

        private boolean submitChunkImpl(PNGChunk chunk) throws IOException {
            if (seenIEND) {
                outOfOrder = true;
                return false;
            }

            // TODO Re-ordering out of order chunks
            // See https://wiki.mozilla.org/APNG_Specification#Chunk_Sequence_Numbers
            if (chunk.hasSeq()) {
                if ((seenIDAT && !seenacTL) || (chunk.getSeq() != seq)) {
                    outOfOrder = true;
                    return false;
                }

                seq++;
            }

            if (shouldFlush(chunk) && !flush()) {
                return false;
            }

            if (chunk.isIEND()) {
                seenIEND = true;
                return true;
            }

            if (chunk.isacTL()) {
                // TODO Does the spec actually forbid multiple acTL chunks?
                if (seenIDAT || seenacTL) {
                    return false;
                }

                seenacTL = true;
                return true;
            }

            if (!setReadState(chunk)) {
                return false;
            }

            if (chunk.data.length > 0) {
                if (readingIDAT) {
                    baos.write(chunk.data);
                } else if (readingfdAT && chunk.isfdAT()) {
                    baos.write(chunk.data, 4, chunk.data.length - 4);
                } else if (chunk.isZLibCompressedNonIdat()) {
                    final byte[] zlibCompressed = chunk.getZLibCompressedNonIdat();

                    if (zlibCompressed != null) {
                        final ZLibFile zlibContainer = new ZLibFile();

                        if (zlibContainer.read(zlibCompressed)) {
                            zlibContainer.deflateStream.setName(chunk.type() + " chunk");
                            helperDeflateStreamMapNonIDAT.put(chunk, zlibContainer);
                        }
                    }
                }
            }

            return true;
        }

        private boolean callFailed = false;

        public boolean submitChunk(PNGChunk chunk) throws IOException {
            if (callFailed) {
                return false;
            }

            boolean submitRes;

            try {
                submitRes = submitChunkImpl(chunk);
            } catch (final IOException e) {
                submitRes = false;
            }

            if (!submitRes) {
                callFailed = true;
                return false;
            }

            return true;
        }

        public boolean goodEndState() {
            return !callFailed && seenIEND && seenIDAT && !readingIDAT && !readingfdAT && !outOfOrder && ((helperFdats == null) || seenacTL);
        }
    }

    @Override
    public boolean read(InputStream is) throws IOException {
        // Check if data is likely PNG
        if ((is == null)
                // Check PNG signature
                || (is.read() != 137) || (is.read() != 'P') || (is.read() != 'N') || (is.read() != 'G') || (is.read() != 13) || (is.read() != 10) || (is.read() != 26) || (is.read() != 10)) {
            return false;
        }

        final PNGChunkHelper helper = new PNGChunkHelper();
        pngChunks = new ArrayList<>();
        PNGChunk lastChunk;

        do {
            final PNGChunk nextChunk = new PNGChunk();

            if (!nextChunk.read(is) || !helper.submitChunk(nextChunk)) {
                idat = helper.helperIdat;
                fdats = helper.helperFdats;
                deflateStreamMapNonIDAT = helper.helperDeflateStreamMapNonIDAT;
                return false;
            }

            pngChunks.add(nextChunk);
            lastChunk = nextChunk;
        } while (!lastChunk.isIEND());

        idat = helper.helperIdat;
        fdats = helper.helperFdats;
        deflateStreamMapNonIDAT = helper.helperDeflateStreamMapNonIDAT;
        return helper.goodEndState();
    }

    @Override
    public String fileType() {
        return "PNG";
    }

    @Override
    public void close() throws IOException {
        IOException e = null;

        try {
            if (idat != null) {
                idat.close();
            }
        } catch (final IOException e1) {
            e = new IOException("Closing PNG failed");
            e.addSuppressed(e1);
        }

        if (fdats != null) {
            for (final ZLibFile file : fdats) {
                try {
                    file.close();
                } catch (final IOException e1) {
                    if (e == null) {
                        e = new IOException("Closing PNG failed");
                    }

                    e.addSuppressed(e1);
                }
            }
        }

        if (deflateStreamMapNonIDAT != null) {
            for (final DeflateFilesContainer container : deflateStreamMapNonIDAT.values()) {
                try {
                    container.close();
                } catch (final IOException e1) {
                    if (e == null) {
                        e = new IOException("Closing PNG failed");
                    }

                    e.addSuppressed(e1);
                }
            }
        }

        if (e != null) {
            throw e;
        }
    }

}
