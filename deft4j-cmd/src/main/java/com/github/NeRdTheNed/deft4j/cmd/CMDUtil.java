package com.github.NeRdTheNed.deft4j.cmd;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.github.NeRdTheNed.deft4j.container.ContainerUtil;
import com.github.NeRdTheNed.deft4j.container.DeflateFilesContainer;
import com.github.NeRdTheNed.deft4j.container.RawDeflateFile;
import com.github.NeRdTheNed.deft4j.deflate.DeflateStream;
import com.github.NeRdTheNed.deft4j.util.Util;
import com.github.NeRdTheNed.deft4j.util.compression.CompressionUtil;
import com.github.NeRdTheNed.deft4j.util.compression.CompressionUtil.Strategy;

public class CMDUtil {
    public enum RecompressMode {
        NONE,
        CHEAP,
        ZOPFLI,
        ZOPFLI_EXTENSIVE,
        ZOPFLI_VERY_EXTENSIVE
    }

    // Debug flags

    // Print stream information
    private static final boolean DEBUG_PRINT = false;
    // Don't optimise at all, just parse and write output
    private static final boolean NO_OPT = false;

    /** Recompress each deflate stream, and optimise. If the result is smaller, use it. */
    private final boolean recompress;
    private final boolean mergeBlocks;

    private final CompressionUtil compUtil;

    CMDUtil(RecompressMode recompressMode, boolean mergeBlocks, int passes) {
        recompress = recompressMode.ordinal() > RecompressMode.NONE.ordinal();
        final boolean zopfli = recompressMode.ordinal() >= RecompressMode.ZOPFLI.ordinal();
        final Strategy strat = recompressMode.ordinal() >= RecompressMode.ZOPFLI_EXTENSIVE.ordinal() ? Strategy.EXTENSIVE : Strategy.MULTI_CHEAP;
        compUtil = recompress ? new CompressionUtil(true, true, recompressMode.ordinal() >= RecompressMode.ZOPFLI_VERY_EXTENSIVE.ordinal(), zopfli, passes, strat, true, true, mergeBlocks) : null;
        this.mergeBlocks = mergeBlocks;
    }

    CMDUtil(RecompressMode recompressMode, boolean mergeBlocks) {
        this(recompressMode, mergeBlocks, 20);
    }

    /** Read from the given input stream into the container, optimise, and write to the output stream */
    private boolean optimise(InputStream is, OutputStream os, DeflateFilesContainer container) throws IOException {
        if (container == null) {
            System.err.println("Invalid file container");
            return false;
        }

        if (container.read(is)) {
            System.out.println("File type recognised as " + container.fileType());

            if (DEBUG_PRINT) {
                System.out.println(container.getStreamInfo());
            }

            final long saved = NO_OPT ? 0 : container.optimise(mergeBlocks);

            if (saved != 0) {
                System.out.println("Saved " + saved + " bits with optimisation");
            }

            if (!NO_OPT && recompress) {
                long recompressSaved = 0;
                final List<DeflateStream> streams = container.getDeflateStreams();
                final int size = streams.size();

                for (int i = 0; i < size; i++) {
                    final DeflateStream stream = streams.get(i);
                    final byte[] uncompressed = stream.getUncompressedData();
                    final byte[] recompresed = compUtil.compress(uncompressed, true);
                    final DeflateStream recompStream = new DeflateStream();
                    final ByteArrayInputStream bais = new ByteArrayInputStream(recompresed);

                    if (recompStream.parse(bais)) {
                        recompStream.optimise(mergeBlocks);
                        final long recompSize = recompStream.getSizeBits();
                        final long originalSize = stream.getSizeBits();
                        final long streamSaved = originalSize - recompSize;

                        if (recompSize < originalSize) {
                            System.out.println("Recompressed stream " + i + " (" + stream.getName() + ")" + " from " + originalSize + " bits to " + recompSize + " bits, saved " + streamSaved + " bits");
                            stream.setFirstBlock(recompStream.getFirstBlock());
                            recompressSaved += streamSaved;
                        }
                    }
                }

                if (recompressSaved > 0) {
                    System.out.println("Saved " + recompressSaved + " bits with recompression");
                }
            }

            if (container.write(os)) {
                return true;
            }

            System.err.println("Failed to write output");
        }

        System.err.println("Invalid " + container.fileType() + " file");
        return false;
    }

    /** Read from the given input file, optimise, and write to the output file */
    public boolean optimiseFile(Path input, Path output, String format, boolean raw) throws IOException {
        // TODO 7z support
        // TODO Minecraft region file support
        // TODO Minecraft anvil file support
        // TODO WOFF support
        // TODO General support for optimising embedded GZip / ZLib / deflate streams in other files
        // I'd be happy to support other deflate based formats
        if (!Files.isRegularFile(input)) {
            System.err.println("Error: Input file does not exist");
            return false;
        }

        if (Files.isDirectory(output)) {
            System.err.println("Error: Output file is a directory");
            return false;
        }

        Path possibleTempPath = output;
        boolean overwrite = false;

        if (Files.isRegularFile(output)) {
            overwrite = true;
            String ext = Util.getFileExtension(input);

            if (ext.isEmpty()) {
                ext = null;
            }

            possibleTempPath = Files.createTempFile("deft-temp-", ext);
            possibleTempPath.toFile().deleteOnExit();
        }

        boolean returnVal = false;

        try
            (final InputStream is = new BufferedInputStream(Files.newInputStream(input));
                    final OutputStream os = new BufferedOutputStream(Files.newOutputStream(possibleTempPath));
                    final DeflateFilesContainer container = raw ? new RawDeflateFile() : format != null ? ContainerUtil.getContainerForExt(format) : ContainerUtil.getContainerForPath(input)) {
            returnVal = optimise(is, os, container);

            if (!returnVal) {
                System.err.println("Failed to optimise input file");
            }
        } catch (final IOException e) {
            System.err.println("IOException thrown when optimising file " + input);
            e.printStackTrace();
        }

        if (overwrite) {
            if (returnVal) {
                Files.copy(possibleTempPath, output, StandardCopyOption.REPLACE_EXISTING);
            }

            try {
                Files.deleteIfExists(possibleTempPath);
            } catch (final Exception e) {
                System.err.println("Issue deleting temporary file " + possibleTempPath);
                e.printStackTrace();
            }
        }

        return returnVal;
    }
}
