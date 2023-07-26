package com.github.NeRdTheNed.deft4j;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.github.NeRdTheNed.deft4j.container.DeflateFilesContainer;
import com.github.NeRdTheNed.deft4j.container.GZFile;
import com.github.NeRdTheNed.deft4j.container.PNGFile;
import com.github.NeRdTheNed.deft4j.container.RawDeflateFile;
import com.github.NeRdTheNed.deft4j.container.ZLibFile;
import com.github.NeRdTheNed.deft4j.container.ZipFile;
import com.github.NeRdTheNed.deft4j.deflate.DeflateStream;
import com.github.NeRdTheNed.deft4j.util.Util;
import com.github.NeRdTheNed.deft4j.util.compression.CompressionUtil;
import com.github.NeRdTheNed.deft4j.util.compression.CompressionUtil.Strategy;

public class Deft {
    /** Optimise a raw deflate stream */
    public static byte[] optimiseDeflateStream(byte[] original) {
        final DeflateStream stream = new DeflateStream();

        try {
            if (stream.parse(original) && (stream.optimise() > 0)) {
                return stream.asBytes();
            }
        } catch (final IOException e) {
            System.err.println("Failed to parse deflate stream data");
            e.printStackTrace();
        }

        return original;
    }

    /** Get the size of a raw deflate stream, in bits. Throws an IOException if it can't be parsed */
    public static long getSizeBits(byte[] deflateStream) throws IOException {
        final DeflateStream stream = new DeflateStream();

        if (!stream.parse(deflateStream)) {
            throw new IOException("Failed to parse deflate stream data");
        }

        return stream.getSizeBits();
    }

    /** Get the size of a raw deflate stream, in bits. If it can't be parsed, round up to the nearest byte of the size of the compressed data. */
    public static long getSizeBitsFallback(byte[] deflateStream) {
        try {
            return getSizeBits(deflateStream);
        } catch (final IOException e) {
            return (long) deflateStream.length * 8;
        }
    }

    /** Attempt to detect the format of a file from magic bytes */
    public static String detectFormat(byte[] bytes) {
        try
            (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            return detectFormat(bais);
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Attempt to detect the format of a file from magic bytes */
    public static String detectFormat(Path path) {
        return detectFormat(path.toFile());
    }

    /** Attempt to detect the format of a file from magic bytes */
    public static String detectFormat(File file) {
        try
            (FileInputStream fis = new FileInputStream(file)) {
            return detectFormat(fis);
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final byte[] pngMagic = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
    private static final byte[] zipMagic = { 0x50, 0x4B, 0x03, 0x04 };
    private static final byte[] zipMagicEmpty = { 0x50, 0x4B, 0x05, 0x06 };
    private static final byte[] zipMagicSpan = { 0x50, 0x4B, 0x07, 0x08 };
    private static final byte[] gzipMagic = { 0x1F, (byte) 0x8B };
    private static final byte[] zlibNo = { 0x78, 0x01 };
    private static final byte[] zlibSpeed = { 0x78, 0x5E };
    private static final byte[] zlibDefault = { 0x78, (byte) 0x9C };
    private static final byte[] zlibBest = { 0x78, (byte) 0xDA };

    private static final byte[][] magics =    { pngMagic, zipMagic, zipMagicEmpty, zipMagicSpan, gzipMagic, zlibNo, zlibSpeed, zlibDefault, zlibBest };
    private static final String[] magicName = { "png",    "zip",    "zip",         "zip",        "gzip",    "zlib", "zlib",    "zlib",      "zlib"   };

    private static final int longestMagic = Arrays.stream(magics).max(Comparator.comparing(o1 -> o1.length)).orElse(pngMagic).length;

    /** Attempt to read file magic, and check if it matches known formats */
    private static String detectFormat(InputStream is) throws IOException {
        final boolean[] isNotPossibleMagic = new boolean[magics.length];

        for (int readBytes = 0; readBytes < longestMagic; readBytes++) {
            final byte read = (byte) is.read();

            for (int i = 0; i < magics.length; i++) {
                if (!isNotPossibleMagic[i]) {
                    final byte[] magic = magics[i];

                    if ((magic.length < (readBytes + 1)) || (read != magic[readBytes])) {
                        isNotPossibleMagic[i] = true;
                    } else if (magic.length <= (readBytes + 1)) {
                        return magicName[i];
                    }
                }
            }
        }

        return null;
    }

    /** Attempts to detect the format of a file, and returns a container suitable for reading it with */
    public static DeflateFilesContainer getContainerForPath(Path path) {
        final String detectedMagic = detectFormat(path);

        if (detectedMagic != null) {
            return getContainerForExt(detectedMagic);
        }

        try {
            final String contentType = Files.probeContentType(path);

            if (contentType != null) {
                switch (contentType) {
                case "application/gzip":
                    return new GZFile();

                case "application/zlib":
                    return new ZLibFile();

                case "application/zip":
                case "application/java-archive":
                case "application/epub+zip":
                case "application/vnd.android.package-archive":
                    return new ZipFile();

                case "image/png":
                    return new PNGFile();

                default:
                    break;
                }
            }
        } catch (final IOException e) {
            // Ignored
        }

        final String pathString = path.toString();
        return getContainerForFilename(pathString);
    }

    /** Attempts to detect the format of a file from only the file name, and returns a container suitable for reading it with */
    public static DeflateFilesContainer getContainerForFilename(String filename) {
        return getContainerForExt(Util.getFileExtension(filename));
    }

    /** Attempts to detect the format of a file from only the extension, and returns a container suitable for reading it with */
    public static DeflateFilesContainer getContainerForExt(String fileExt) {
        switch (fileExt.toLowerCase()) {
        case "gz":
        case "gzip":
        case "tgz":
        case "taz":
        case "svgz":
        case "cpgz":
        case "wmz":
        case "emz":

        // TODO NBT files may also be in a ZLib container
        case "dat":
        case "nbt":
        case "mine":
        case "mclevel":
            return new GZFile();

        case "zlib":
        case "zz":
            return new ZLibFile();

        case "zip":
        case "jar":
        case "apk":
        case "ipa":
        case "ear":
        case "war":
        case "epub":
            return new ZipFile();

        case "png":
            return new PNGFile();

        default:
            return new RawDeflateFile();
        }
    }

    public enum RecompressMode {
        NONE,
        CHEAP,
        ZOPFLI,
        ZOPFLI_EXTENSIVE,
        ZOPFLI_VERY_EXTENSIVE
    }

    // Debug flags

    // Print optimisation information
    public static final boolean PRINT_OPT = true;
    public static final boolean PRINT_OPT_FINE = PRINT_OPT && false;
    public static final boolean PRINT_OPT_FINER = PRINT_OPT_FINE && false;

    // Print stream information
    private static final boolean DEBUG_PRINT = false;
    // Don't optimise at all, just parse and write output
    private static final boolean NO_OPT = false;

    /** Recompress each deflate stream, and optimise. If the result is smaller, use it. */
    private final boolean recompress;

    private final CompressionUtil compUtil;

    Deft(RecompressMode recompressMode) {
        recompress = recompressMode.ordinal() > RecompressMode.NONE.ordinal();
        final boolean zopfli = recompressMode.ordinal() >= RecompressMode.ZOPFLI.ordinal();
        final Strategy strat = recompressMode.ordinal() >= RecompressMode.ZOPFLI_EXTENSIVE.ordinal() ? Strategy.EXTENSIVE : Strategy.MULTI_CHEAP;
        compUtil = !recompress ? null : new CompressionUtil(true, true, recompressMode.ordinal() >= RecompressMode.ZOPFLI_VERY_EXTENSIVE.ordinal(), zopfli, strat, true);
    }

    /** Read from the given input stream into the container, optimise, and write to the output stream */
    public boolean optimise(InputStream is, OutputStream os, DeflateFilesContainer container) throws IOException {
        if (container.read(is)) {
            System.out.println("File type recognised as " + container.fileType());

            if (DEBUG_PRINT) {
                System.out.println(container.getStreamInfo());
            }

            final long saved = NO_OPT ? 0 : container.optimise();

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
                    final byte[] recompresed = compUtil.compress(uncompressed, false);
                    final DeflateStream recompStream = new DeflateStream();
                    final ByteArrayInputStream bais = new ByteArrayInputStream(recompresed);

                    if (recompStream.parse(bais)) {
                        recompStream.optimise();
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

            if ("".equals(ext)) {
                ext = null;
            }

            possibleTempPath = Files.createTempFile("deft-temp-", ext);
            possibleTempPath.toFile().deleteOnExit();
        }

        boolean returnVal = false;

        try
            (InputStream is = new BufferedInputStream(new FileInputStream(input.toFile()));
                    OutputStream os = new BufferedOutputStream(new FileOutputStream(possibleTempPath.toFile()))) {
            final DeflateFilesContainer container = raw ? new RawDeflateFile() : format != null ? getContainerForExt(format) : getContainerForPath(input);
            returnVal = optimise(is, os, container);

            if (!returnVal) {
                System.err.println("Failed to optimise input file");
            }
        } catch (final IOException e) {
            System.err.println("IOException thrown when optimising file " + input);
            e.printStackTrace();
        }

        if (returnVal && overwrite) {
            Files.copy(possibleTempPath, output, StandardCopyOption.REPLACE_EXISTING);
        }

        return returnVal;
    }
}
