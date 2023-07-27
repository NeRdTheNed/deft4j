package com.github.NeRdTheNed.deft4j.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringJoiner;

/** Static utility methods which don't require any dependencies */
public final class Util {
    private static String binStr(char value) {
        return String.valueOf((int) value);
    }

    /** Check if the byte array only contains this value */
    public static boolean checkAllValue(byte[] bytes, byte value) {
        for (final byte b : bytes) {
            if (b != value) {
                return false;
            }
        }

        return true;
    }

    /** Check if the byte array only contains zero */
    public static boolean checkAllZero(byte[] bytes) {
        for (final byte b : bytes) {
            if (b != 0) {
                return false;
            }
        }

        return true;
    }

    /** Creates a new array with the contents of both arrays */
    public static int[] combine(int[] a1, int[] a2) {
        final int[] combined = new int[a1.length + a2.length];
        System.arraycopy(a1, 0, combined, 0, a1.length);
        System.arraycopy(a2, 0, combined, a1.length, a2.length);
        return combined;
    }

    /** Reads the contents of an input stream to a byte array */
    public static byte[] convertInputStreamToBytes(InputStream in) throws IOException {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        int length;

        while ((length = in.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }

        return result.toByteArray();
    }

    /** Returns the extension of this file, or an empty String if it doesn't have one */
    public static String getFileExtension(File file) {
        final String name = file.getName();
        final int extLocation = name.lastIndexOf(".");
        return extLocation > 0 ? name.substring(extLocation) : "";
    }

    /** Returns the extension of this file, or an empty String if it doesn't have one */
    public static String getFileExtension(Path path) {
        return getFileExtension(path.toFile());
    }

    /** Returns the extension of this file, or an empty String if it doesn't have one */
    public static String getFileExtension(String fileName) {
        return getFileExtension(new File(fileName));
    }

    /** Rough "is printable character" check */
    public static boolean isPrintable(char value) {
        return !Character.isISOControl(value) && (Character.isLetterOrDigit(value) || Character.isWhitespace(value) || Character.isValidCodePoint(value));
    }

    /** Gets n least significant bits */
    public static long lsb(long x, int n) {
        assert n <= 63;
        return x & (((long)1 << n) - 1);
    }

    /** Gets a printable string representation of a character array similar to Arrays.toString */
    public static String printableStr(byte[] values) {
        return printableStr(values, false);
    }

    /** Gets a printable string representation of a character array similar to Arrays.toString, calls String.valueOf on values if bin is true */
    public static String printableStr(byte[] values, boolean bin) {
        final StringJoiner sj = new StringJoiner(", ", "[", "]");

        for (final byte value : values) {
            sj.add(printableStr((char) value, bin));
        }

        return sj.toString();
    }

    /** Gets a printable string representation of a character */
    public static String printableStr(char value) {
        if (isPrintable(value)) {
            return "\'" + value + "\'";
        }

        return binStr(value);
    }

    /** Gets a printable string representation of a character, or calls String.valueOf if bin is true */
    public static String printableStr(char value, boolean bin) {
        if (bin) {
            return binStr(value);
        }

        return printableStr(value);
    }

    /** Read n bytes from the input stream */
    public static byte[] readFromInputStream(InputStream is, long n) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (int i = 0; i < n; i++) {
            baos.write((byte) is.read());
        }

        return baos.toByteArray();
    }

    /** Read a C-style null terminated String from an InputStream */
    public static String readStr(InputStream is) throws IOException {
        final ByteArrayOutputStream boas = new ByteArrayOutputStream();
        int read = is.read();

        while (read != 0) {
            boas.write(read);
            read = is.read();
        }

        return new String(boas.toByteArray());
    }

    /** Reverses n bits, truncating */
    public static int rev(int bits, int size) {
        int rev = 0;

        while (size >= 0) {
            size--;
            rev |= (bits & 1) << size;
            bits >>= 1;
        }

        return rev;
    }

    /** Reverses the byte order, truncating to 32 bits */
    public static long revByteOrder32(long val) {
        return ((val & 0xFF) << 24) | (((val >>> 8) & 0xFF) << 16) | (((val >>> 16) & 0xFF) << 8) | ((val >>> 24) & 0xFF);
    }

    /** C-style-ish strlen */
    public static int strlen(byte[] arr, int offset) {
        int i;

        for (i = offset; (i < arr.length) && (arr[i] != 0); i++) {
            // This space left intentionally blank
        }

        return i - offset;
    }

    /** Writes a byte array to a file */
    public static boolean writeFile(File file, byte[] bytes) {
        try {
            Files.write(file.toPath(), bytes);
            return true;
        } catch (final IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Writes a byte array to a file */
    public static boolean writeFile(String file, byte[] bytes) {
        return writeFile(new File(file), bytes);
    }

    /**
     * Write an integer to the output stream as bytes in BE order.
     *
     * @param out the output stream
     * @param value the value to write
     */
    public static void writeIntBE(OutputStream out, int value) throws IOException {
        out.write((value >> 24) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    /**
     * Write an integer to the output stream as bytes in LE order.
     *
     * @param out the output stream
     * @param value the value to write
     */
    public static void writeIntLE(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

    /**
     * Write a short to the output stream as bytes in LE order.
     *
     * @param out the output stream
     * @param value the value to write
     */
    public static void writeShortLE(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    /** Private constructor to hide the default one */
    private Util() {
        // This space left intentionally blank
    }
}