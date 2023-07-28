package com.github.NeRdTheNed.deft4j.container;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.github.NeRdTheNed.deft4j.Deft;
import com.github.NeRdTheNed.deft4j.deflate.DeflateStream;

public interface DeflateFilesContainer {
    boolean RECALC = true;

    /** Optimise all given streams. Returns the total amount of bits saved. */
    static long optimise(List<DeflateStream> streams) {
        long savedTotal = 0;
        final int size = streams.size();

        for (int defStream = 0; defStream < size; defStream++) {
            final DeflateStream stream = streams.get(defStream);

            if (Deft.PRINT_OPT_FINE) {
                System.out.println("Stream " + defStream + " (" + stream.getName() + ")");
            }

            final long saved = stream.optimise();

            if (Deft.PRINT_OPT && (saved > 0)) {
                System.out.println(saved + " bits saved in stream " + defStream + " (" + stream.getName() + ")");
            }

            savedTotal += saved;
        }

        if (Deft.PRINT_OPT && (savedTotal > 0)) {
            System.out.println("Total bits saved " + savedTotal);
        }

        return savedTotal;
    }

    List<DeflateStream> getDeflateStreams();

    boolean write(OutputStream os) throws IOException;
    boolean read(InputStream is) throws IOException;

    default boolean read(byte[] bytes) throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return read(bais);
    }

    /** Optimise all streams in this container. Returns the total amount of bits saved. */
    default long optimise() {
        return optimise(getDeflateStreams());
    }

    /** Returns debug information for the given stream */
    static String getStreamInfo(DeflateStream stream) {
        return stream.printBlockInfo();
    }

    /** Returns debug information for all given streams */
    static String getStreamsInfo(List<DeflateStream> streams) {
        final StringBuilder sb = new StringBuilder();
        final int size = streams.size();
        int defStream;

        for (defStream = 0; defStream < size; defStream++) {
            final DeflateStream stream = streams.get(defStream);
            sb.append("Stream ").append(defStream).append('\n').append(getStreamInfo(stream));
        }

        sb.append("\nTotal streams: " + defStream);
        return sb.toString();
    }

    /** Returns debug information for all streams in this container */
    default String getStreamInfo() {
        return "File type: " + fileType() + "\nDeflate streams info:\n" + getStreamsInfo(getDeflateStreams());
    }

    String fileType();
}
