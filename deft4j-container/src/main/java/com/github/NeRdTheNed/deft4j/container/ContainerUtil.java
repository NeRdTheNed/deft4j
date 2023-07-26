package com.github.NeRdTheNed.deft4j.container;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

import com.github.NeRdTheNed.deft4j.util.Util;

public class ContainerUtil {
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
}
