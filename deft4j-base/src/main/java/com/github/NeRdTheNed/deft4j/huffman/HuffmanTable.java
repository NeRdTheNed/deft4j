package com.github.NeRdTheNed.deft4j.huffman;

import java.util.ArrayList;
import java.util.List;

import com.github.NeRdTheNed.deft4j.util.Util;

/**
 * Implements a Huffman code table.
 * @author Ridge Shrubsall (21112211)
 */
public class HuffmanTable {
    /**
     * An array of codes.
     */
    public int[] code;

    /**
     * An array of codelengths.
     */
    public int[] codeLen;

    /**
     * Create a new Huffman table.
     * @param numSymbols The total number of symbols
     */
    public HuffmanTable(int numSymbols) {
        code = new int[numSymbols];
        codeLen = new int[numSymbols];
    }

    /**
     * Pack the given codelength arrays.
     * @param litCodeLen The literal codelengths
     * @param distCodeLen The distance codelengths
     * @param ohh Use combinations of RLE lengths instead of just greedy matching. Based on code by Frédéric Kayser.
     * @param use8 Use 4 + 4 instead of 6 + single + single
     * @param use7 Use 4 + 3 instead of 6 + single
     * @param alt8 If use8, use 5 + 3 instead of 4 + 4
     * @return The packed codelengths
     */
    public static List<Integer> packCodeLengths(int[] litCodeLen, int[] distCodeLen, boolean ohh, boolean use8, boolean use7, boolean alt8, boolean noRep, boolean noZRep, boolean noZRep2) {
        final List<Integer> lengths = new ArrayList<>();
        pack(lengths, Util.combine(litCodeLen, distCodeLen), ohh, use8, use7, alt8, noRep, noZRep, noZRep2);
        return lengths;
    }

    // TODO Try other combinations (5 & 3 etc)
    private static final int FIRST_8 = 4;
    private static final int SECOND_8 = 4;

    private static final int ALT_FIRST_8 = 5;
    private static final int ALT_SECOND_8 = 3;

    private static final int FIRST_7 = 4;
    private static final int SECOND_7 = 3;

    /**
     * Pack an array of codelengths.
     * (see RFC 1951, section 3.2.7)
     *
     * TODO Options to disable use of specific RLE codes
     * @param lengths The list of length symbols
     * @param codeLen The codelengths to be packed
     * @param ohh Use combinations of RLE lengths instead of just greedy matching. Based on code by Frédéric Kayser.
     * @param use8 Use 4 + 4 instead of 6 + single + single
     * @param use7 Use 4 + 3 instead of 6 + single
     * @param alt8 If use8, use 5 + 3 instead of 4 + 4
     */
    private static void pack(List<Integer> lengths, int[] codeLen, boolean ohh, boolean use8, boolean use7, boolean alt8, boolean noRep, boolean noZRep, boolean noZRep2) {
        final int n = codeLen.length;
        // Perform a run-length encoding
        int last = codeLen[0];                         // Get the first length value
        int runLength = 1;

        for (int i = 1; i <= n; i++) {
            if ((i < n) && (codeLen[i] == last)) {         // Find the number of repeat occurrences
                runLength++;
            } else {
                if (last == 0) {                           // Is the length zero/unused?
                    if (!noZRep2) {
                        int j = 138;

                        while (j >= 11) {
                            if ((runLength - j) >= 0) {    // Encode 11 to 138 repeats of zero
                                lengths.add(18);
                                lengths.add(j - 11);
                                runLength -= j;
                            } else {
                                j--;
                            }
                        }
                    }

                    if (!noZRep) {
                        int j = 10;

                        while (j >= 3) {
                            if ((runLength - j) >= 0) {    // Encode 3 to 10 repeats of zero
                                lengths.add(17);
                                lengths.add(j - 3);
                                runLength -= j;
                            } else {
                                j--;
                            }
                        }
                    }
                }

                if (!noRep && (runLength > 0)) {
                    lengths.add(last);                     // Write the length value
                    runLength--;
                    int j = 6;

                    while (j >= 3) {
                        if (ohh) {
                            // Use 4 + 4 instead of 6 + single + single
                            if (use8 && (runLength == 8)) {
                                lengths.add(16);
                                lengths.add((alt8 ? ALT_FIRST_8 : FIRST_8) - 3);
                                lengths.add(16);
                                lengths.add((alt8 ? ALT_SECOND_8 : SECOND_8) - 3);
                                runLength -= 8;
                                break;
                            }

                            // Use 4 + 3 instead of 6 + single
                            if (use7 && (runLength == 7)) {
                                lengths.add(16);
                                lengths.add(FIRST_7 - 3);
                                lengths.add(16);
                                lengths.add(SECOND_7 - 3);
                                runLength -= 7;
                                break;
                            }
                        }

                        if ((runLength - j) >= 0) {    // Encode 3 to 6 repeat lengths
                            lengths.add(16);
                            lengths.add(j - 3);
                            runLength -= j;
                        } else {
                            j--;
                        }
                    }
                }

                while (runLength > 0) {                // Write the remaining length(s)
                    lengths.add(last);
                    runLength--;
                }

                if (i < n) {                           // Get the next length value
                    last = codeLen[i];
                    runLength = 1;
                }
            }
        }
    }


    /*
     * Default Huffman code tables
     * (see RFC 1951, section 3.2.6)
     */
    public static final HuffmanTable LIT;
    public static final HuffmanTable DIST;
    static {
        // Generate fixed literal codes
        LIT = new HuffmanTable(286);
        int nextCode = 0;

        for (int i = 256; i <= 279; i++) {
            LIT.code[i] = nextCode;
            nextCode++;
            LIT.codeLen[i] = 7;
        }

        nextCode <<= 1;

        for (int i = 0; i <= 143; i++) {
            LIT.code[i] = nextCode;
            nextCode++;
            LIT.codeLen[i] = 8;
        }

        for (int i = 280; i <= 285; i++) {
            LIT.code[i] = nextCode;
            nextCode++;
            LIT.codeLen[i] = 8;
        }

        nextCode += 2;
        nextCode <<= 1;

        for (int i = 144; i <= 255; i++) {
            LIT.code[i] = nextCode;
            nextCode++;
            LIT.codeLen[i] = 9;
        }

        // Generate fixed distance codes
        DIST = new HuffmanTable(30);

        for (int i = 0; i <= 29; i++) {
            DIST.code[i] = i;
            DIST.codeLen[i] = 5;
        }
    }
}
