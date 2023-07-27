package com.github.NeRdTheNed.deft4j.deflate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.NeRdTheNed.deft4j.Deft;
import com.github.NeRdTheNed.deft4j.huffman.Huffman;
import com.github.NeRdTheNed.deft4j.huffman.Huffman.DecodedSym;
import com.github.NeRdTheNed.deft4j.huffman.HuffmanTable;
import com.github.NeRdTheNed.deft4j.huffman.HuffmanTree;
import com.github.NeRdTheNed.deft4j.io.BitInputStream;
import com.github.NeRdTheNed.deft4j.io.BitOutputStream;
import com.github.NeRdTheNed.deft4j.util.Util;

public class DeflateBlockHuffman extends DeflateBlock {
    private DeflateBlockType type;

    private Huffman litlenDec;
    private Huffman distDec;

    // TODO LinkedList?
    private List<LitLen> litlens = new ArrayList<>();
    // Decoded data
    private boolean finishedDec = false;
    private byte[] decodedData;
    private long dataPos;
    private long sizeBits;

    // Dynamic header info
    private Huffman codeLenDec;
    private int numLitlenLens;
    private int numDistLens;
    private int numCodelenLens;
    private int[] codelenLengths;

    private List<LitLen> rlePairsLitlen;
    private List<LitLen> rlePairsDist;

    private long dynamicHeaderSizeBits;

    public DeflateBlockHuffman(DeflateBlock prevBlock, DeflateBlockType type) {
        super(prevBlock);

        if ((type != DeflateBlockType.FIXED) && (type != DeflateBlockType.DYNAMIC)) {
            throw new IllegalArgumentException("Illegal type " + type + " for huffman compressed block");
        }

        this.type = type;
    }

    @Override
    public DeflateBlockType getDeflateBlockType() {
        return type;
    }

    /** Read a slice of decompressed data from the current block or previous blocks, with overlapping backref support. Supports reading slices during decoding. */
    @Override
    byte[] readSlice(long backDist, long len) {
        if (!finishedDec) {
            return readSlice(backDist, len, this, decodedData, (int) dataPos);
        }

        return super.readSlice(backDist, len);
    }

    private void writeTemp(byte[] vals) {
        assert !finishedDec;

        for (final byte b : vals) {
            writeTemp(b);
        }
    }

    private void writeTemp(int val) {
        assert !finishedDec;

        if (decodedData.length < (dataPos + 1)) {
            // Haha malloc go brrr
            final byte[] newArr = new byte[decodedData.length * 2];
            System.arraycopy(decodedData, 0, newArr, 0, decodedData.length);
            decodedData = newArr;
        }

        decodedData[(int) dataPos] = (byte) val;
        dataPos++;
    }

    private void startTemp() {
        assert !finishedDec;
        decodedData = new byte[0x100];
        dataPos = 0;
        finishedDec = false;
    }

    private void finishTemp() {
        assert !finishedDec;
        finishedDec = true;
        // Resize array to decoded data size
        final byte[] finalArray = Arrays.copyOf(decodedData, (int) dataPos);
        decodedData = finalArray;
    }

    // Debug print flags
    private static final boolean DEBUG_PRINT_OPT = Deft.PRINT_OPT_FINER;
    private static final boolean DEBUG_PRINT_OPT_REFREPLACE = DEBUG_PRINT_OPT;
    private static final boolean DEBUG_PRINT_OPT_UNUSED = DEBUG_PRINT_OPT;
    private static final boolean DEBUG_PRINT_OPT_UNUSED_CODELENS = DEBUG_PRINT_OPT_UNUSED;

    private static final String DEBUG_PRINT_OPT_PRE = "Optimisation ";
    private static final String DEBUG_PRINT_OPT_POST = ": ";

    private static final String MATCH_REPLACE = "replace backrefs with literals if shorter";
    private static final String RUN_REPLACE = "replace RLE runs with literals if shorter";
    private static final String DYNAMIC_HEADER = "dynamic header";

    private static final String REMOVE_TRALING = DYNAMIC_HEADER + " remove trailing ";
    private static final String REMOVE_TRALING_CODELENS = REMOVE_TRALING + "zero-length codelens";

    private static final String DEBUG_PRINT_OPT_REFREPLACE_STR = DEBUG_PRINT_OPT_PRE + MATCH_REPLACE + DEBUG_PRINT_OPT_POST;
    private static final String DEBUG_PRINT_OPT_RUNREPLACE_STR = DEBUG_PRINT_OPT_PRE + RUN_REPLACE + DEBUG_PRINT_OPT_POST;
    private static final String DEBUG_PRINT_OPT_UNUSED_CODELENS_STR = DEBUG_PRINT_OPT_PRE + REMOVE_TRALING_CODELENS + DEBUG_PRINT_OPT_POST;

    /**
     * Replaces backrefs / RLE encoded codelens with literals
     * if the encoded size of the backref / RLE encoded codelen is larger than
     * the encoded size of equivalent literal sequence.
     *
     * TODO Replace matches with submatches if smaller (e.g. if two matches are smaller than one)
     * @param print allow debug printing
     * @param prune remove matches if the length is the same as well
     */
    private static long replaceWithLiteralsIfSmaller(List<LitLen> checkLitlens, Huffman decoder, boolean print, String optPrefix, boolean prune) {
        long savedTotal = 0;
        final ListIterator<LitLen> litIter = checkLitlens.listIterator();

        while (litIter.hasNext()) {
            final LitLen check = litIter.next();

            // Replace matches with literals if smaller
            if (check.dist != 0) {
                assert check.decodedVal != null;
                boolean skip = false;
                final List<LitLen> replace = new ArrayList<>();
                int totalSize = 0;

                for (final byte bNeg : check.decodedVal) {
                    final int b = bNeg & 0xFF;
                    final LitLen lit = new LitLen(b);
                    lit.decodedVal = new byte[] { bNeg };
                    lit.encodedSize = decoder.getSymLen(b);

                    if (lit.encodedSize < 1) {
                        // No symbol for this literal
                        // TODO Add code if it saves space overall
                        if (DEBUG_PRINT_OPT_REFREPLACE && print) {
                            System.out.println(optPrefix + "Possible missed optimisation: no literal code for " + b);
                        }

                        skip = true;
                        break;
                    }

                    totalSize += lit.encodedSize;
                    replace.add(lit);
                }

                final boolean remove = prune ? totalSize <= check.encodedSize : totalSize < check.encodedSize;

                if (!skip && remove) {
                    if (DEBUG_PRINT_OPT_REFREPLACE && print) {
                        System.out.println(optPrefix + "Found size " + check.encodedSize + ", replacing with size " + totalSize);
                        System.out.println(optPrefix + "Original: " + check + "\nNew:");

                        for (final LitLen rep : replace) {
                            System.out.println(rep);
                        }
                    }

                    final int saved = check.encodedSize - totalSize;
                    assert (prune && (saved >= 0)) || (saved > 0);
                    savedTotal += saved;
                    litIter.remove();

                    for (final LitLen rep : replace) {
                        litIter.add(rep);
                    }

                    if (DEBUG_PRINT_OPT_REFREPLACE && print) {
                        System.out.println(optPrefix + "Did replace, saved " + saved);
                    }
                }
            }
        }

        return savedTotal;
    }

    private void replaceBackrefsWithLiteralsIfSmaller(boolean prune, boolean print) {
        sizeBits -= replaceWithLiteralsIfSmaller(litlens, litlenDec, print, DEBUG_PRINT_OPT_REFREPLACE_STR, prune);
    }

    private void replaceRLERunsWithLiteralsIfSmaller(boolean prune, boolean print) {
        if (type != DeflateBlockType.DYNAMIC) {
            return;
        }

        long savedHeader = 0;
        savedHeader += replaceWithLiteralsIfSmaller(rlePairsLitlen, codeLenDec, print, DEBUG_PRINT_OPT_RUNREPLACE_STR, prune);
        savedHeader += replaceWithLiteralsIfSmaller(rlePairsDist, codeLenDec, print, DEBUG_PRINT_OPT_RUNREPLACE_STR, prune);
        sizeBits -= savedHeader;
        dynamicHeaderSizeBits -= savedHeader;
    }

    /** Removes trailing zero-length codelens from the codelen lengths */
    private long removeDynHeaderTrailingZeroLenCodelens(boolean print) {
        if (type != DeflateBlockType.DYNAMIC) {
            return 0;
        }

        int lastZero = -1;
        int lastNonZero = numCodelenLens;

        for (int i = 0; i < numCodelenLens; i++) {
            final int codelenLength = codelenLengths[Constants.codelen_lengths_order[i]];

            if (codelenLength == 0) {
                lastZero = i;
            } else {
                lastNonZero = i;
            }
        }

        if (lastZero > lastNonZero) {
            numCodelenLens = lastZero;

            if (DEBUG_PRINT_OPT_UNUSED_CODELENS && print) {
                System.out.println(DEBUG_PRINT_OPT_UNUSED_CODELENS_STR + "Removed zero length code " + Constants.codelen_lengths_order[lastZero] + " at index " + lastZero);
            }

            return 3 + removeDynHeaderTrailingZeroLenCodelens(print);
        }

        return 0;
    }

    public void removeTrailingHeaderCodes(boolean print) {
        final long savedHeader = removeDynHeaderTrailingZeroLenCodelens(print);
        sizeBits -= savedHeader;
        dynamicHeaderSizeBits -= savedHeader;
    }

    @Override
    public long optimise() {
        // TODO Merge blocks with same huffman codes
        // TODO Remove unused codes / lengths
        // TODO Backrefs / check if sequence is in backref codebook
        final long original = sizeBits;
        replaceBackrefsWithLiteralsIfSmaller(false, true);
        removeTrailingHeaderCodes(true);
        replaceRLERunsWithLiteralsIfSmaller(false, true);
        return original - sizeBits;
    }

    private static final boolean DEBUG_PRINT_HEADER_RECODE = false;

    public void rewriteHeader() {
        rewriteHeader(true, true, true, false);
    }

    public void rewriteHeader(boolean ohh, boolean use8, boolean use7, boolean alt8) {
        if (type != DeflateBlockType.DYNAMIC) {
            return;
        }

        if (DEBUG_PRINT_HEADER_RECODE) {
            System.out.println("Original header size: " + dynamicHeaderSizeBits + "\nOriginal size: " + sizeBits);
        }

        sizeBits -= dynamicHeaderSizeBits;
        dynamicHeaderSizeBits = 0;
        numLitlenLens = litlenDec.table.codeLen.length;
        numDistLens = distDec.table.codeLen.length;
        rlePairsLitlen = new ArrayList<>();
        rlePairsDist = new ArrayList<>();
        final List<Integer> repack = HuffmanTable.packCodeLengths(litlenDec.table.codeLen, distDec.table.codeLen, ohh, use8, use7, alt8);
        codeLenDec = Huffman.ofRLEPacked(repack);
        //codelenLengths = new int[Constants.MAX_CODELEN_LENS];
        //System.arraycopy(codeLenDec.table.codeLen, 0, codelenLengths, 0, codeLenDec.table.codeLen.length);
        codelenLengths = codeLenDec.table.codeLen;
        numCodelenLens = Constants.MAX_CODELEN_LENS;
        dynamicHeaderSizeBits = 5 + 5 + 4 + ((long) numCodelenLens * 3);
        int i = 0;
        final Iterator<Integer> iter = repack.iterator();
        LitLen prePair = null;

        while (i < (numLitlenLens + numDistLens)) {
            final int sym = iter.next();
            final LitLen rlePair = new LitLen(sym);
            rlePair.encodedSize = codeLenDec.getSymLen(sym);
            dynamicHeaderSizeBits += rlePair.encodedSize;
            (i >= numLitlenLens ? rlePairsDist : rlePairsLitlen).add(rlePair);

            if ((sym >= 0) && (sym <= Constants.CODELEN_MAX_LIT)) {
                rlePair.decodedVal = new byte[] { (byte) sym };
                i++;
            } else {
                int dist = iter.next();

                switch (sym) {
                case Constants.CODELEN_COPY: {
                    // 2 bits + 3
                    dist += Constants.CODELEN_COPY_MIN;
                    rlePair.encodedSize += 2;
                    dynamicHeaderSizeBits += 2;
                    rlePair.dist = dist;
                    rlePair.decodedVal = new byte[dist];
                    assert prePair != null;
                    assert (rlePair.dist >= Constants.CODELEN_COPY_MIN) && (rlePair.dist <= Constants.CODELEN_COPY_MAX);
                    Arrays.fill(rlePair.decodedVal, prePair.decodedVal[prePair.decodedVal.length - 1]);
                    break;
                }

                case Constants.CODELEN_ZEROS: {
                    // 3 bits + 3
                    dist += Constants.CODELEN_ZEROS_MIN;
                    rlePair.encodedSize += 3;
                    dynamicHeaderSizeBits += 3;
                    rlePair.dist = dist;
                    rlePair.decodedVal = new byte[dist];
                    assert (rlePair.dist >= Constants.CODELEN_ZEROS_MIN) &&
                    (rlePair.dist <= Constants.CODELEN_ZEROS_MAX);
                    Arrays.fill(rlePair.decodedVal, (byte) 0);
                    break;
                }

                case Constants.CODELEN_ZEROS2: {
                    // 7 bits + 138
                    dist += Constants.CODELEN_ZEROS2_MIN;
                    rlePair.encodedSize += 7;
                    dynamicHeaderSizeBits += 7;
                    rlePair.dist = dist;
                    rlePair.decodedVal = new byte[dist];
                    assert (rlePair.dist >= Constants.CODELEN_ZEROS2_MIN) &&
                    (rlePair.dist <= Constants.CODELEN_ZEROS2_MAX);
                    Arrays.fill(rlePair.decodedVal, (byte) 0);
                    break;
                }

                default:
                    // Invalid symbol
                    throw new RuntimeException("Invalid RLE symbol when encoding dynamic header");
                }

                i += dist;
            }

            prePair = rlePair;
        }

        assert !iter.hasNext();
        sizeBits += dynamicHeaderSizeBits;
        removeTrailingHeaderCodes(false);

        if (DEBUG_PRINT_HEADER_RECODE) {
            System.out.println("New header size: " + dynamicHeaderSizeBits + "\nNew size: " + sizeBits);
        }
    }

    public void recodeHeader() {
        if (type != DeflateBlockType.DYNAMIC) {
            return;
        }

        sizeBits -= dynamicHeaderSizeBits;
        dynamicHeaderSizeBits = 0;
        final List<LitLen> rlePairsComb = Stream.concat(rlePairsLitlen.stream(), rlePairsDist.stream()).collect(Collectors.toList());
        final List<Integer> lengths = new ArrayList<>();
        /*numLitlenLens = 0;
        numDistLens = 0;
        final Set<Integer> uniqueLit = new HashSet<>();
        final Set<Integer> uniqueDist = new HashSet<>();

        for (final LitLen litlenThis : litlens) {
            if (litlenThis.dist != 0) {
                uniqueLit.add(Constants.len2litlen[(int) litlenThis.litlen]);
                uniqueDist.add(Constants.distance2dist(litlenThis.dist));
            } else {
                uniqueLit.add((int) litlenThis.litlen);
            }
        }

        numLitlenLens = Math.max(uniqueLit.size(), Constants.MIN_LITLEN_LENS);
        numDistLens = Math.max(uniqueDist.size(), Constants.MIN_DIST_LENS);*/
        int rleTotal = 0;

        for (final LitLen rlePair : rlePairsComb) {
            lengths.add((int) rlePair.litlen);

            if (rlePair.dist > 0) {
                lengths.add((int) rlePair.dist);
                rleTotal += (int) rlePair.dist;
            } else {
                rleTotal++;
            }
        }

        assert (numLitlenLens + numDistLens) == rleTotal;
        codeLenDec = Huffman.ofRLEPacked(lengths);
        //codelenLengths = new int[Constants.MAX_CODELEN_LENS];
        //System.arraycopy(codeLenDec.table.codeLen, 0, codelenLengths, 0, codeLenDec.table.codeLen.length);
        codelenLengths = codeLenDec.table.codeLen;
        removeDynHeaderTrailingZeroLenCodelens(false);
        dynamicHeaderSizeBits = 5 + 5 + 4 + ((long) numCodelenLens * 3);

        for (final LitLen rlePair : rlePairsComb) {
            rlePair.encodedSize = codeLenDec.getSymLen((int) rlePair.litlen);

            if (rlePair.dist > 0) {
                switch ((int) rlePair.litlen) {
                case Constants.CODELEN_COPY: {
                    // 2 bits + 3
                    rlePair.encodedSize += 2;
                    break;
                }

                case Constants.CODELEN_ZEROS: {
                    // 3 bits + 3
                    rlePair.encodedSize += 3;
                    break;
                }

                case Constants.CODELEN_ZEROS2: {
                    // 7 bits + 138
                    rlePair.encodedSize += 7;
                    break;
                }

                default:
                    // Invalid symbol
                    throw new RuntimeException("Invalid RLE symbol when encoding dynamic header");
                }
            }

            dynamicHeaderSizeBits += rlePair.encodedSize;
        }

        sizeBits += dynamicHeaderSizeBits;
    }

    /** Attempts to replace RLE matches that take the same size or more than literals in the dynamic header, then re-codes the header */
    public void recodeHeaderToLessRLEMatches() {
        replaceRLERunsWithLiteralsIfSmaller(true, false);
        recodeHeader();
    }

    public void recodeToFixedHuffman() {
        if (type == DeflateBlockType.FIXED) {
            return;
        }

        sizeBits -= dynamicHeaderSizeBits;
        type = DeflateBlockType.FIXED;
        dynamicHeaderSizeBits = 0;
        codeLenDec = null;
        numLitlenLens = 0;
        numDistLens = 0;
        numCodelenLens = 0;
        codelenLengths = null;
        rlePairsLitlen = null;
        rlePairsDist = null;
        recodeToHuffmanInternal(Huffman.FIXED_LITLEN_INST, Huffman.FIXED_DIST_INST);
    }

    public void recodeHuffmanLessMatches() {
        replaceBackrefsWithLiteralsIfSmaller(true, false);
        recodeHuffman();
    }

    /**
     * This can legally be 0 according to the deflate spec,
     * but some decoders need larger values.
     * Set this to 0 for the smallest possible output.
     * Set this to 1 to work around a bug with Zlib 1.2.1 and older.
     * Set this to 2 to work around other buggy decoders.
     */
    private static final int MIN_DIST_CODES = 2;
    private static final int MIN_LIT_CODES = 0;

    public void recodeHuffman() {
        final int[] litFreqTemp = new int[Constants.MAX_LITLEN_LENS - 2];
        final int[] distFreqTemp = new int[Constants.MAX_DIST_LENS - 2];

        for (final LitLen litlenThis : litlens) {
            if (litlenThis.dist > 0) {
                litFreqTemp[Constants.len2litlen[(int) litlenThis.litlen]]++;
                distFreqTemp[Constants.distance2dist(litlenThis.dist)]++;
            } else {
                litFreqTemp[(int) litlenThis.litlen]++;
            }
        }

        int lastNonZeroLit = litFreqTemp.length;

        while ((lastNonZeroLit > 0) && (litFreqTemp[lastNonZeroLit - 1] == 0)) {
            lastNonZeroLit--;
        }

        int realLastNonZeroLit = lastNonZeroLit;

        if (lastNonZeroLit < MIN_LIT_CODES) {
            lastNonZeroLit = MIN_LIT_CODES;
        }

        int lastNonZeroDist = distFreqTemp.length;

        while ((lastNonZeroDist > 0) && (distFreqTemp[lastNonZeroDist - 1] == 0)) {
            lastNonZeroDist--;
        }

        int realLastNonZeroDist = lastNonZeroDist;

        if (lastNonZeroDist < MIN_DIST_CODES) {
            lastNonZeroDist = MIN_DIST_CODES;
        }

        final int[] litFreq = Arrays.copyOf(litFreqTemp, lastNonZeroLit);
        final int[] distFreq = Arrays.copyOf(distFreqTemp, lastNonZeroDist);
        int i = 0;

        while (realLastNonZeroLit < MIN_LIT_CODES) {
            if (litFreq[i] == 0) {
                litFreq[i] = 1;
                realLastNonZeroLit++;
            }

            i++;
        }

        i = 0;

        while (realLastNonZeroDist < MIN_DIST_CODES) {
            if (distFreq[i] == 0) {
                distFreq[i] = 1;
                realLastNonZeroDist++;
            }

            i++;
        }

        final Huffman newLit = new Huffman(new HuffmanTree(litFreq, 15).getTable());
        final Huffman newDist = new Huffman(new HuffmanTree(distFreq, 15).getTable());
        recodeToHuffman(newLit, newDist);
    }

    public void recodeToHuffman(Huffman newLitlenDec, Huffman newDistDec) {
        recodeToHuffman(newLitlenDec, newDistDec, true, true, true, false);
    }

    public void recodeToHuffman(Huffman newLitlenDec, Huffman newDistDec, boolean ohh, boolean use8, boolean use7, boolean alt8) {
        if ((newLitlenDec == Huffman.FIXED_LITLEN_INST) && (newDistDec == Huffman.FIXED_DIST_INST)) {
            recodeToFixedHuffman();
        } else {
            type = DeflateBlockType.DYNAMIC;
            recodeToHuffmanInternal(newLitlenDec.copy(), newDistDec.copy());
            rewriteHeader(ohh, use8, use7, alt8);
        }
    }

    private void recodeToHuffmanInternal(Huffman newLitlenDec, Huffman newDistDec) {
        litlenDec = newLitlenDec;
        distDec = newDistDec;

        for (final LitLen litlenThis : litlens) {
            sizeBits -= litlenThis.encodedSize;

            if (litlenThis.dist > 0) {
                final long distance = litlenThis.dist;
                final long len = litlenThis.litlen;
                final long litlen = Constants.len2litlen[(int) len];
                // litlen bits
                long nbits = litlenDec.getSymLen((int) litlen);
                // ebits
                nbits += Constants.litlen_tbl[(int) (litlen - Constants.LITLEN_TBL_OFFSET)].ebits;
                // Back reference distance
                final long dist = Constants.distance2dist(distance);
                // dist bits
                nbits += distDec.getSymLen((int) dist);
                // ebits
                nbits += Constants.dist_tbl[(int) dist].ebits;
                litlenThis.encodedSize = (int) nbits;
            } else {
                litlenThis.encodedSize = litlenDec.getSymLen((int) litlenThis.litlen);
            }

            sizeBits += litlenThis.encodedSize;
        }
    }

    // Debug flags
    // Print litlen parsing info
    private static final boolean DEBUG_PRINT_PARSE = false;
    // Try to print the parsed data as text
    private static final boolean DEBUG_PRINT_TEXT = false;

    private boolean decodeStream(BitInputStream is) throws IOException {
        startTemp();

        if (DEBUG_PRINT_PARSE) {
            System.out.println("Begin litlen parsing");
        }

        while (true) {
            final DecodedSym litlenDecSym = litlenDec.readSym(is);
            final int litlen = litlenDecSym.decoded;

            if ((litlen < 0) || (litlen > Constants.LITLEN_MAX)) {
                // Failed to decode
                return false;
            }

            if (litlen <= 0xff) {
                // Literal
                if (DEBUG_PRINT_PARSE) {
                    System.out.println("Literal " + Util.printableStr((char) litlen, !DEBUG_PRINT_TEXT));
                }

                final LitLen readLit = new LitLen(litlen);
                final byte decByte = (byte) litlen;
                readLit.decodedVal = new byte[] { decByte };
                readLit.encodedSize = litlenDecSym.codeLen;
                sizeBits += readLit.encodedSize;
                litlens.add(readLit);
                writeTemp(litlen);
                continue;
            }

            if (litlen == Constants.LITLEN_EOB) {
                // EOB
                if (DEBUG_PRINT_PARSE) {
                    System.out.println("EOB");
                }

                final LitLen readLit = new LitLen(litlen);
                readLit.encodedSize = litlenDecSym.codeLen;
                sizeBits += readLit.encodedSize;
                litlens.add(readLit);
                finishTemp();
                return true;
            }

            // Backref
            int totalSize = litlenDecSym.codeLen;
            assert (litlen >= Constants.LITLEN_TBL_OFFSET) && (litlen <= Constants.LITLEN_MAX);
            long len   = Constants.litlen_tbl[litlen - Constants.LITLEN_TBL_OFFSET].baseLen;
            long ebits = Constants.litlen_tbl[litlen - Constants.LITLEN_TBL_OFFSET].ebits;

            if (ebits != 0) {
                totalSize += ebits;
                len += is.readBits((int) ebits);
            }

            assert (len >= Constants.MIN_LEN) && (len <= Constants.MAX_LEN);
            // Get the distance
            final DecodedSym distsymDecSym = distDec.readSym(is);
            final int distsym = distsymDecSym.decoded;
            totalSize += distsymDecSym.codeLen;

            if ((distsym < 0) || (distsym > Constants.DISTSYM_MAX)) {
                // Failed to decode, or invalid symbol
                return false;
            }

            long dist  = Constants.dist_tbl[distsym].baseDist;
            ebits = Constants.dist_tbl[distsym].ebits;

            if (ebits != 0) {
                totalSize += ebits;
                dist += is.readBits((int) ebits);
            }

            assert (dist >= Constants.MIN_DISTANCE) && (dist <= Constants.MAX_DISTANCE);
            final LitLen readLen = new LitLen(dist, len);
            readLen.encodedSize = totalSize;
            sizeBits += totalSize;

            // Print the first half of the debug message now,
            // because this code was historically buggy and sometimes caused issues after this
            if (DEBUG_PRINT_PARSE) {
                System.out.print("Backref distance " + dist + " size " + len + " ");
            }

            readLen.decodedVal = readSlice(dist, len);

            if (litlens.contains(readLen)) {
                readLen.decodedVal = litlens.get(litlens.indexOf(readLen)).decodedVal;
            }

            litlens.add(readLen);
            writeTemp(readLen.decodedVal);

            // Print the other half of the message
            if (DEBUG_PRINT_PARSE) {
                System.out.println("decoded " + Util.printableStr(readLen.decodedVal, !DEBUG_PRINT_TEXT));
            }
        }
    }

    private boolean initDynamicDecoder(BitInputStream is) throws IOException {
        // HLIT
        numLitlenLens = (int) is.readBits(5) + Constants.MIN_LITLEN_LENS;
        assert numLitlenLens <= Constants.MAX_LITLEN_LENS;
        // HDIST
        numDistLens = (int) is.readBits(5) + Constants.MIN_DIST_LENS;
        assert numDistLens <= Constants.MAX_DIST_LENS;
        // HCLEN
        numCodelenLens = (int) is.readBits(4) + Constants.MIN_CODELEN_LENS;
        assert numCodelenLens <= Constants.MAX_CODELEN_LENS;
        codelenLengths = new int[Constants.MAX_CODELEN_LENS];

        // Read the codelen codeword lengths (3 bits each) and initialize the codelen decoder
        for (int i = 0; i < numCodelenLens; i++) {
            codelenLengths[Constants.codelen_lengths_order[i]] = (int) is.readBits(3);
        }

        dynamicHeaderSizeBits = 5 + 5 + 4 + ((long) numCodelenLens * 3);
        codeLenDec = Huffman.ofCodelens(codelenLengths);
        // TODO temporary attempt at reducing memory use
        codelenLengths = codeLenDec.table.codeLen;
        final int[] codeLengths = new int[Constants.MAX_LITLEN_LENS + Constants.MAX_DIST_LENS];
        rlePairsLitlen = new ArrayList<>();
        rlePairsDist = new ArrayList<>();
        int i = 0;

        while (i < (numLitlenLens + numDistLens)) {
            final DecodedSym decodedSym = codeLenDec.readSym(is);
            dynamicHeaderSizeBits += decodedSym.codeLen;
            final int sym = decodedSym.decoded;
            final LitLen rlePair = new LitLen(sym);
            rlePair.encodedSize = decodedSym.codeLen;
            (i >= numLitlenLens ? rlePairsDist : rlePairsLitlen).add(rlePair);

            if ((sym >= 0) && (sym <= Constants.CODELEN_MAX_LIT)) {
                // A literal codeword length
                codeLengths[i] = sym;
                i++;
                rlePair.decodedVal = new byte[] { (byte) sym };
            } else {
                switch (sym) {
                case Constants.CODELEN_COPY: {
                    // Copy the previous codeword length 3--6 times
                    if (i < 1) {
                        return false; // No previous length
                    }

                    // 2 bits + 3
                    int n = (int) is.readBits(2) + Constants.CODELEN_COPY_MIN;
                    dynamicHeaderSizeBits += 2;
                    rlePair.encodedSize += 2;
                    rlePair.dist = n;
                    assert (n >= Constants.CODELEN_COPY_MIN) && (n <= Constants.CODELEN_COPY_MAX);

                    if ((i + n) > (numLitlenLens + numDistLens)) {
                        return false;
                    }

                    rlePair.decodedVal = new byte[n];
                    Arrays.fill(rlePair.decodedVal, (byte) codeLengths[i - 1]);

                    while (n-- != 0) {
                        codeLengths[i] = codeLengths[i - 1];
                        i++;
                    }

                    break;
                }

                case Constants.CODELEN_ZEROS: {
                    // 3--10 zeros; 3 bits + 3
                    int n = (int) is.readBits(3) + Constants.CODELEN_ZEROS_MIN;
                    dynamicHeaderSizeBits += 3;
                    rlePair.encodedSize += 3;
                    rlePair.dist = n;
                    assert (n >= Constants.CODELEN_ZEROS_MIN) &&
                    (n <= Constants.CODELEN_ZEROS_MAX);

                    if ((i + n) > (numLitlenLens + numDistLens)) {
                        return false;
                    }

                    rlePair.decodedVal = new byte[n];
                    Arrays.fill(rlePair.decodedVal, (byte) 0);

                    while (n-- != 0) {
                        codeLengths[i] = 0;
                        i++;
                    }

                    break;
                }

                case Constants.CODELEN_ZEROS2: {
                    // 11--138 zeros; 7 bits + 138
                    int n = (int) is.readBits(7) + Constants.CODELEN_ZEROS2_MIN;
                    dynamicHeaderSizeBits += 7;
                    rlePair.encodedSize += 7;
                    rlePair.dist = n;
                    assert (n >= Constants.CODELEN_ZEROS2_MIN) &&
                    (n <= Constants.CODELEN_ZEROS2_MAX);

                    if ((i + n) > (numLitlenLens + numDistLens)) {
                        return false;
                    }

                    rlePair.decodedVal = new byte[n];
                    Arrays.fill(rlePair.decodedVal, (byte) 0);

                    while (n-- != 0) {
                        codeLengths[i] = 0;
                        i++;
                    }

                    break;
                }

                default:
                    // Invalid symbol
                    return false;
                }
            }
        }

        final int[] litlenCodeLengths = new int[numLitlenLens];
        final int[] distCodeLengths = new int[numDistLens];
        System.arraycopy(codeLengths, 0, litlenCodeLengths, 0, numLitlenLens);
        System.arraycopy(codeLengths, numLitlenLens, distCodeLengths, 0, numDistLens);
        litlenDec = Huffman.ofCodelens(litlenCodeLengths);
        distDec = Huffman.ofCodelens(distCodeLengths);
        sizeBits += dynamicHeaderSizeBits;
        return true;
    }

    private boolean initStaticDecoder() {
        litlenDec = Huffman.FIXED_LITLEN_INST;
        distDec = Huffman.FIXED_DIST_INST;
        return true;
    }

    @Override
    public boolean parse(BitInputStream is) throws IOException {
        sizeBits = 0;

        if (type == DeflateBlockType.DYNAMIC) {
            if (!initDynamicDecoder(is)) {
                return false;
            }
        } else if (!initStaticDecoder()) {
            return false;
        }

        return decodeStream(is);
    }

    private boolean writeHuffCode(BitOutputStream os) throws IOException {
        // HLIT
        assert (numLitlenLens <= Constants.MAX_LITLEN_LENS) && (numLitlenLens >= Constants.MIN_LITLEN_LENS);
        os.writeNBits((long) numLitlenLens - Constants.MIN_LITLEN_LENS, 5);
        // HDIST
        assert (numDistLens <= Constants.MAX_DIST_LENS) && (numDistLens >= Constants.MIN_DIST_LENS);
        os.writeNBits((long) numDistLens - Constants.MIN_DIST_LENS, 5);
        // HCLEN
        assert (numCodelenLens <= Constants.MAX_CODELEN_LENS) && (numCodelenLens >= Constants.MIN_CODELEN_LENS);
        os.writeNBits((long) numCodelenLens - Constants.MIN_CODELEN_LENS, 4);

        // Codelen lengths
        for (int i = 0; i < numCodelenLens; i++) {
            final int codelenLength = codelenLengths[Constants.codelen_lengths_order[i]];
            os.writeNBits(codelenLength, 3);
        }

        int i = 0;

        // Encoded dynamic code lengths
        for (final LitLen rlePair : Stream.concat(rlePairsLitlen.stream(), rlePairsDist.stream()).collect(Collectors.toList())) {
            final long sym = rlePair.litlen;
            os.writeNBits(codeLenDec.getSym((int) sym), codeLenDec.getSymLen((int) sym));

            if (rlePair.dist == 0) {
                i++;
                assert sym <= Constants.CODELEN_MAX_LIT;
            } else {
                final int writeDistOffset;
                final int writeDistSize;

                switch ((int) sym) {
                case Constants.CODELEN_COPY: {
                    // 2 bits + 3
                    assert (rlePair.dist >= Constants.CODELEN_COPY_MIN) && (rlePair.dist <= Constants.CODELEN_COPY_MAX);
                    writeDistOffset = Constants.CODELEN_COPY_MIN;
                    writeDistSize = 2;
                    break;
                }

                case Constants.CODELEN_ZEROS: {
                    // 3 bits + 3
                    assert (rlePair.dist >= Constants.CODELEN_ZEROS_MIN) &&
                    (rlePair.dist <= Constants.CODELEN_ZEROS_MAX);
                    writeDistOffset = Constants.CODELEN_ZEROS_MIN;
                    writeDistSize = 3;
                    break;
                }

                case Constants.CODELEN_ZEROS2: {
                    // 7 bits + 138
                    assert (rlePair.dist >= Constants.CODELEN_ZEROS2_MIN) &&
                    (rlePair.dist <= Constants.CODELEN_ZEROS2_MAX);
                    writeDistOffset = Constants.CODELEN_ZEROS2_MIN;
                    writeDistSize = 7;
                    break;
                }

                default:
                    // Invalid symbol
                    return false;
                }

                os.writeNBits(rlePair.dist - writeDistOffset, writeDistSize);
                i += rlePair.dist;
            }
        }

        assert i == (numLitlenLens + numDistLens);
        return true;
    }

    private void writeSym(BitOutputStream os, long val) throws IOException {
        assert val <= Constants.LITLEN_EOB;
        os.writeNBits(litlenDec.getSym((int) val), litlenDec.getSymLen((int) val));
    }

    private void writeBackref(BitOutputStream os, long len, long distance) throws IOException {
        // Back reference length
        final long litlen = Constants.len2litlen[(int) len];
        // litlen bits
        long bits = litlenDec.getSym((int) litlen);
        long nbits = litlenDec.getSymLen((int) litlen);
        // ebits
        long ebits = len - Constants.litlen_tbl[(int) (litlen - Constants.LITLEN_TBL_OFFSET)].baseLen;
        bits |= ebits << nbits;
        nbits += Constants.litlen_tbl[(int) (litlen - Constants.LITLEN_TBL_OFFSET)].ebits;
        // Back reference distance
        final long dist = Constants.distance2dist(distance);
        // dist bits
        bits |= (long) distDec.getSym((int) dist) << nbits;
        nbits += distDec.getSymLen((int) dist);
        // ebits
        ebits = distance - Constants.dist_tbl[(int) dist].baseDist;
        bits |= ebits << nbits;
        nbits += Constants.dist_tbl[(int) dist].ebits;
        os.writeNBits(bits, (int) nbits);
    }

    private void writeLitLen(BitOutputStream os, LitLen litlenThis) throws IOException {
        if (litlenThis.dist == 0) {
            writeSym(os, litlenThis.litlen);
        } else {
            writeBackref(os, litlenThis.litlen, litlenThis.dist);
        }
    }

    private boolean writeDefBlock(BitOutputStream os) throws IOException {
        for (final LitLen litlenThis : litlens) {
            writeLitLen(os, litlenThis);
        }

        return true;
    }

    private boolean writeProlog(BitOutputStream os, boolean finalBlock) throws IOException {
        final int finalBlockI = finalBlock ? 1 : 0;
        os.writeNBits((type.ordinal() << 1) | finalBlockI, 3);
        return (type == DeflateBlockType.FIXED) || writeHuffCode(os);
    }

    @Override
    public boolean write(BitOutputStream os, boolean finalBlock) throws IOException {
        return writeProlog(os, finalBlock) && writeDefBlock(os);
    }

    @Override
    public byte[] getUncompressedData() {
        return decodedData;
    }

    // TODO
    @Override
    public boolean fromUncompressed(byte[] input) {
        return false;
    }

    @Override
    public long getSizeBits(long alginment) {
        return sizeBits;
    }

    @Override
    public DeflateBlock copy() {
        final DeflateBlockHuffman compressedBlock = new DeflateBlockHuffman(getPrevious(), type);
        copy(this, compressedBlock);
        compressedBlock.litlenDec = litlenDec.copy();
        compressedBlock.distDec = distDec.copy();

        for (final LitLen litlen : litlens) {
            compressedBlock.litlens.add(litlen.copy());
        }

        compressedBlock.finishedDec = true;
        compressedBlock.dataPos = dataPos;
        compressedBlock.sizeBits = sizeBits;
        //compressedBlock.decodedData = new byte[decodedData.length];
        //System.arraycopy(decodedData, 0, compressedBlock.decodedData, 0, decodedData.length);
        compressedBlock.decodedData = decodedData;

        if (type == DeflateBlockType.DYNAMIC) {
            compressedBlock.dynamicHeaderSizeBits = dynamicHeaderSizeBits;
            compressedBlock.codeLenDec = codeLenDec.copy();
            compressedBlock.numLitlenLens = numLitlenLens;
            compressedBlock.numDistLens = numDistLens;
            compressedBlock.numCodelenLens = numCodelenLens;
            //compressedBlock.codelenLengths = new int[codelenLengths.length];
            //System.arraycopy(codelenLengths, 0, compressedBlock.codelenLengths, 0, codelenLengths.length);
            compressedBlock.codelenLengths = codelenLengths;
            compressedBlock.rlePairsLitlen = new ArrayList<>();
            compressedBlock.rlePairsDist = new ArrayList<>();

            for (final LitLen rlePair : rlePairsLitlen) {
                compressedBlock.rlePairsLitlen.add(rlePair.copy());
            }

            for (final LitLen rlePair : rlePairsDist) {
                compressedBlock.rlePairsDist.add(rlePair.copy());
            }
        }

        return compressedBlock;
    }

    @Override
    public void discard() {
        super.discard();
        litlenDec = null;
        distDec = null;
        decodedData = null;
        codeLenDec = null;
        codelenLengths = null;
        /*if (litlens != null) {
            try {
                litlens.clear();
            } catch (final Exception e) {
                // Ignored
            }
        }*/
        litlens = null;
        /*if (rlePairsLitlen != null) {
            try {
                rlePairsLitlen.clear();
            } catch (final Exception e) {
                // Ignored
            }
        }*/
        rlePairsLitlen = null;
        /*if (rlePairsDist != null) {
            try {
                rlePairsDist.clear();
            } catch (final Exception e) {
                // Ignored
            }
        }*/
        rlePairsDist = null;
    }
}
