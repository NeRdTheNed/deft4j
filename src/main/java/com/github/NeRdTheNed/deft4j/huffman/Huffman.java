package com.github.NeRdTheNed.deft4j.huffman;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.compress.utils.BitInputStream;

import com.github.NeRdTheNed.deft4j.deflate.Constants;
import com.github.NeRdTheNed.deft4j.util.Util;

/* Based on code by Ridge Shrubsall */
public class Huffman {
    public Huffman copy() {
        if ((this == FIXED_LITLEN_INST) || (this == FIXED_DIST_INST)) {
            return this;
        }

        return Huffman.ofCodelens(table.codeLen);
    }

    public static final Huffman FIXED_LITLEN_INST = ofFixedLitlen();
    public static final Huffman FIXED_DIST_INST = ofFixedDist();

    /**
     * Build a list of canonical codes from the given codelengths.
     * @param codeLen The codelength array
     * @return The list of codes
     */
    private static int[] buildCodes(int[] codeLen) {
        final int n = codeLen.length;
        final int[] codes = new int[n];
        // Find used codelengths
        final Set<Integer> lengthSet = new TreeSet<>();

        for (int i = 0; i < n; i++) {
            if (codeLen[i] > 0) {
                lengthSet.add(codeLen[i]);
            }
        }

        // Build the codes
        int nextCode = 0;
        int lastShift = 0;

        for (final Integer length : lengthSet) {
            nextCode <<= length - lastShift;
            lastShift = length;

            for (int i = 0; i < n; i++) {
                if (codeLen[i] == length) {
                    codes[i] = nextCode;
                    nextCode++;
                }
            }
        }

        return codes;
    }

    private static Map<Integer, List<Integer>> buildCodeMap(List<Integer> codes, int[] codeLen) {
        final int n = codeLen.length;
        final Map<Integer, List<Integer>> codeMap = new TreeMap<>();

        // Build the codemap
        for (int i = 0; i < n; i++) {
            final int len = codeLen[i];

            if (len > 0) {
                List<Integer> codeList = codeMap.get(len);

                if (codeList == null) {
                    codeList = new ArrayList<>();
                    codeMap.put(codeLen[i], codeList);
                }

                codeList.add(codes.get(i));
            }
        }

        return codeMap;
    }

    public final HuffmanTable table;

    /**
     * The current set of literal codes.
     */
    private final List<Integer> codes;
    private final Map<Integer, List<Integer>> codeMap;

    public Huffman(HuffmanTable table) {
        this.table = table;
        codes = new ArrayList<>();

        for (int i = 0; i < this.table.code.length; i++) {
            // Janky hack mate!
            // If this is 0, then it replaces the code 0.
            int element = -1;

            if (this.table.codeLen[i] > 0) {
                element = this.table.code[i];
            }

            codes.add(element);
        }

        codeMap = buildCodeMap(codes, this.table.codeLen);
    }

    public static Huffman ofRLEPacked(List<Integer> lengths) {
        final int[] lenFreq = new int[Constants.MAX_CODELEN_LENS];
        final Iterator<Integer> iter = lengths.iterator();

        while (iter.hasNext()) {
            final int s = iter.next();
            lenFreq[s]++;

            // Skip run-length bits
            if ((s == 16) || (s == 17) || (s == 18)) {
                iter.next();
            }
        }

        final HuffmanTree tree = new HuffmanTree(lenFreq, 7);
        final HuffmanTable table = tree.getTable();
        return new Huffman(table);
    }

    public static Huffman ofCodelens(int[] codelens) {
        final HuffmanTable table = new HuffmanTable(codelens.length);
        System.arraycopy(codelens, 0, table.codeLen, 0, codelens.length);
        final int[] codes = buildCodes(codelens);
        System.arraycopy(codes, 0, table.code, 0, codes.length);
        return new Huffman(table);
    }

    private static Huffman ofFixedLitlen() {
        return new Huffman(HuffmanTable.LIT);
    }

    private static Huffman ofFixedDist() {
        return new Huffman(HuffmanTable.DIST);
    }

    public static class DecodedSym {
        public final int code;
        public final int codeLen;
        public final int decoded;

        public DecodedSym(int code, int codeLen, int decoded) {
            this.code = code;
            this.codeLen = codeLen;
            this.decoded = decoded;
        }
    }

    /**
     * Read a symbol from the input stream.
     * @param codes The code list to use
     * @param codeMap The code map to use
     * @return The decoded symbol
     */
    public static DecodedSym readSymbol(List<Integer> codes, Map<Integer, List<Integer>> codeMap, BitInputStream is) throws IOException {
        int code = 0;
        int codeLen = 0;
        int index = -1;

        // Start reading bits
        do {
            if (codeLen == 15) {
                throw new AssertionError("Couldn't find code " + code + " len " + codeLen);
            }

            // Read one bit
            code <<= 1;
            final int bitRead = (int) is.readBits(1);
            code |= bitRead;
            codeLen++;
            // Check if we have a matching code
            final List<Integer> codeList = codeMap.get(codeLen);

            if (codeList != null) {
                index = codeList.indexOf(code);
            }
        } while (index == -1);

        // Return the index of the code (which equals the symbol value)
        return new DecodedSym(code, codeLen, codes.indexOf(code));
    }

    public DecodedSym readSym(BitInputStream is) throws IOException {
        return readSymbol(codes, codeMap, is);
    }

    public int decodeSym(BitInputStream is) throws IOException {
        return readSym(is).decoded;
    }

    public int getSym(int n) {
        return Util.rev(table.code[n], table.codeLen[n]);
    }

    public int getSymLen(int n) {
        return table.codeLen[n];
    }
}
