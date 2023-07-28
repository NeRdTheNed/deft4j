package com.github.NeRdTheNed.deft4j.deflate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import com.github.NeRdTheNed.deft4j.Deft;
import com.github.NeRdTheNed.deft4j.io.BitInputStream;
import com.github.NeRdTheNed.deft4j.io.BitOutputStream;
import com.github.NeRdTheNed.deft4j.util.Pair;

public class DeflateStream {
    private static final String DEFAULT_NAME = "unnamed stream";
    private final String name;

    public DeflateStream(String name) {
        if (name == null) {
            name = DEFAULT_NAME;
        }

        this.name = name;
    }

    public DeflateStream() {
        this(DEFAULT_NAME);
    }

    /** Returns debug information about each block */
    public String printBlockInfo() {
        DeflateBlock currentBlock = getFirstBlock();
        long pos = 0;
        int block = 0;
        final StringBuilder blockStats = new StringBuilder();

        while (currentBlock != null) {
            pos += 3;
            final long blockSize = currentBlock.getSizeBits(pos);
            blockStats.append('\n').append("Block ").append(block).append(" position ").append(pos - 3).append(" size ").append(blockSize + 3).append(" type ").append(currentBlock.getDeflateBlockType());
            pos += currentBlock.getSizeBits(pos);
            block++;
            currentBlock = currentBlock.getNext();
        }

        final StringBuilder sb = new StringBuilder().append("Stream name: ").append(getName()).append("\nBlock info:").append(blockStats).append("\nTotal blocks: ").append(block);
        return sb.toString();
    }

    public String getName() {
        return name;
    }

    private DeflateBlock firstBlock;

    public boolean parse(long dataLength, byte[] data, int offset) throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(data, offset, (int) dataLength);
        return parse(bais);
    }

    public boolean parse(byte[] data) throws IOException {
        return parse(data.length, data, 0);
    }

    public boolean parse(InputStream is) throws IOException {
        final BitInputStream bis = new BitInputStream(is);
        long bits;
        boolean bfinal;
        DeflateBlock prevBlock = null;
        long pos = 0;
        boolean first = true;

        do {
            bits = bis.readBits(3);
            pos += 3;
            bfinal = (bits & 1) != 0;
            bits >>>= 1;
            DeflateBlock newBlock;

            switch ((int) bits) {
            case 0b00:
                // Uncompressed
                newBlock = new DeflateBlockUncompressed(prevBlock);
                break;

            case 0b01:
                // Fixed
                newBlock = new DeflateBlockHuffman(prevBlock, DeflateBlockType.FIXED);
                break;

            case 0b10:
                // Dynamic
                newBlock = new DeflateBlockHuffman(prevBlock, DeflateBlockType.DYNAMIC);
                break;

            default:
                // Reserved
                return false;
            }

            if (!newBlock.parse(bis)) {
                return false;
            }

            if (prevBlock != null) {
                prevBlock.setNext(newBlock);
            }

            pos += newBlock.getSizeBits(pos);
            prevBlock = newBlock;

            if (first) {
                setFirstBlock(newBlock);
                first = false;
            }
        } while (!bfinal);

        return true;
    }

    public boolean write(OutputStream os) throws IOException {
        final BitOutputStream bos = new BitOutputStream(os);
        DeflateBlock currentBlock = getFirstBlock();

        while (currentBlock != null) {
            final DeflateBlock nextBlock = currentBlock.getNext();
            final boolean finalBlock = nextBlock == null;

            if (!currentBlock.write(bos, finalBlock)) {
                return false;
            }

            currentBlock = nextBlock;
        }

        bos.flushToByteAligned();
        return true;
    }

    public DeflateBlock getFirstBlock() {
        return firstBlock;
    }

    public void setFirstBlock(DeflateBlock newBlock) {
        if (firstBlock != null) {
            firstBlock.replace(newBlock);
        }

        firstBlock = newBlock;
    }

    public byte[] getUncompressedData() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflateBlock currentBlock = getFirstBlock();

        while (currentBlock != null) {
            baos.write(currentBlock.getUncompressedData());
            currentBlock = currentBlock.getNext();
        }

        return baos.toByteArray();
    }

    public long getSizeBits() {
        long size = 0;
        DeflateBlock currentBlock = getFirstBlock();

        while (currentBlock != null) {
            size += 3;
            size += currentBlock.getSizeBits(size);
            currentBlock = currentBlock.getNext();
        }

        return size;
    }

    private static DeflateBlockHuffman optimiseBlockDynBlock(DeflateBlockHuffman block, boolean pre, boolean ohh, boolean use8, boolean use7, boolean alt8) {
        if (block.getDeflateBlockType() != DeflateBlockType.DYNAMIC) {
            return null;
        }

        final DeflateBlockHuffman optimised = (DeflateBlockHuffman) block.copy();

        if (pre) {
            optimised.optimise();
        }

        optimised.rewriteHeader(ohh, use8, use7, alt8);
        optimised.optimise();
        return optimised;
    }

    private static DeflateBlockHuffman recodedHuffman(DeflateBlockHuffman block, boolean prune) {
        final DeflateBlockHuffman recoded = (DeflateBlockHuffman) block.copy();

        if (prune) {
            recoded.recodeHuffmanLessMatches();
        } else {
            recoded.recodeHuffman();
        }

        return recoded;
    }

    private static final boolean TRY_ALT_8 = false;

    private static final boolean DEFAULT_8 = false;
    private static final boolean ALT_8 = !DEFAULT_8;

    private static void addOptimisedRecoded(Consumer<Pair<? extends DeflateBlockHuffman, String>> callback, DeflateBlockHuffman toOptimise, String baseName) {
        final Map<DeflateBlockHuffman, String> blocks = new LinkedHashMap<>();
        blocks.put(toOptimise, baseName);
        blocks.put(recodedHuffman(toOptimise, false), baseName + "huffman-recoded ");
        blocks.put(recodedHuffman(toOptimise, true), baseName + "huffman-recoded-pruned ");

        for (final Entry<DeflateBlockHuffman, String> entry : blocks.entrySet()) {
            final DeflateBlockHuffman block = entry.getKey();
            final String name = entry.getValue();

            for (final boolean pre : new boolean[] {true, false}) {
                for (final boolean ohh : new boolean[] {true, false}) {
                    if (ohh) {
                        for (final boolean alt8 : (TRY_ALT_8 ? new boolean[] {DEFAULT_8, ALT_8} : new boolean[] {DEFAULT_8})) {
                            for (final boolean use8 : new boolean[] {true, false}) {
                                for (final boolean use7 : new boolean[] {true, false}) {
                                    if (!use8 && (alt8 || !use7)) {
                                        continue;
                                    }

                                    final String newName = name + (pre ? "recoded-optimised" : "optimised-recoded") + (ohh ? " ohh" : "") + (use8 ? alt8 ? " alt-optimise-8" : " optimise-8" : "") + (use7 ? " optimise-7" : "");
                                    final DeflateBlockHuffman opt = optimiseBlockDynBlock(block, pre, ohh, use8, use7, alt8);
                                    callback.accept(new Pair<>(opt, newName));
                                }
                            }
                        }
                    } else {
                        final String newName = name + (pre ? "recoded-optimised" : "optimised-recoded") + (ohh ? " ohh" : "");
                        final DeflateBlockHuffman opt = optimiseBlockDynBlock(block, pre, ohh, true, true, false);
                        callback.accept(new Pair<>(opt, newName));
                    }
                }
            }
        }
    }

    private static DeflateBlock optimiseBlockNormal(DeflateBlock block) {
        final DeflateBlock optimised = block.copy();
        optimised.optimise();
        return optimised;
    }

    private static DeflateBlockHuffman toFixedHuffman(DeflateBlockHuffman block) {
        if (block.getDeflateBlockType() != DeflateBlockType.DYNAMIC) {
            return null;
        }

        final DeflateBlockHuffman fixed = (DeflateBlockHuffman) block.copy();
        fixed.recodeToFixedHuffman();
        return fixed;
    }

    // Debug print flags
    private static final boolean PRINT_OPT_FINE = Deft.PRINT_OPT_FINE;
    private static final boolean PRINT_OPT_FINER = Deft.PRINT_OPT_FINER;

    private static DeflateBlock optimiseBlock(DeflateBlock toOptimise, long position) {
        if (PRINT_OPT_FINER) {
            System.out.println("Optimising block " + toOptimise);
        }

        final Pair<DeflateBlock, Long> currentSmallestPair = new Pair<>(toOptimise, toOptimise.getSizeBits(position));
        final Consumer<Pair<? extends DeflateBlock, String>> callback = e -> {
            if (PRINT_OPT_FINER) {
                System.out.println("Trying " + e.v);
            }

            final DeflateBlock candidate = e.k;
            final long newSizeBits = candidate.getSizeBits(position);

            if (newSizeBits < currentSmallestPair.v) {
                if (PRINT_OPT_FINE) {
                    System.out.println("Candidate " + e.v + " saved " + (currentSmallestPair.v - newSizeBits) + " bits");
                }

                if (currentSmallestPair.k != toOptimise) {
                    //currentSmallestPair.k.discard();
                }

                currentSmallestPair.k = candidate;
                currentSmallestPair.v = newSizeBits;
            }
        };
        // Standard
        final DeflateBlock optimised = optimiseBlockNormal(toOptimise);
        callback.accept(new Pair<>(optimised, "optimised"));

        if (toOptimise.getDeflateBlockType() != DeflateBlockType.STORED) {
            // Uncompressed
            callback.accept(new Pair<>(toOptimise.asUncompressed(), "uncompressed"));
        }

        if (toOptimise.getDeflateBlockType() == DeflateBlockType.DYNAMIC) {
            final Consumer<Pair<? extends DeflateBlockHuffman, String>> runOptimisationsCallback = toFixed -> {
                // Fixed huffman block
                final String name = toFixed.v + " fixed-huffman";
                final DeflateBlockHuffman fixed = toFixedHuffman(toFixed.k);
                fixed.optimise();
                callback.accept(new Pair<>(fixed, name));

                // Post recoded header
                final String namePost = toFixed.v + " post-recoded";
                final DeflateBlockHuffman post = (DeflateBlockHuffman) toFixed.k.copy();
                post.recodeHeader();
                callback.accept(new Pair<>(post, namePost));
                callback.accept(new Pair<>(optimiseBlockNormal(post), namePost + " optimised"));
                addOptimisedRecoded(callback::accept, post, namePost + " ");

                // RLE pruned header
                final String namePrune = toFixed.v + " pruned";
                final DeflateBlockHuffman prune = (DeflateBlockHuffman) toFixed.k.copy();
                prune.recodeHeaderToLessRLEMatches();
                callback.accept(new Pair<>(prune, namePrune));
                callback.accept(new Pair<>(optimiseBlockNormal(prune), namePrune + " optimised"));
                addOptimisedRecoded(callback::accept, prune, namePrune + " ");
            };
            final DeflateBlockHuffman toOptimiseHuffman = (DeflateBlockHuffman) toOptimise;
            final DeflateBlockHuffman optimisedHuffman = (DeflateBlockHuffman) optimised;
            runOptimisationsCallback.accept(new Pair<>(toOptimiseHuffman, "default"));
            runOptimisationsCallback.accept(new Pair<>(optimisedHuffman, "optimised"));
            addOptimisedRecoded(e -> { callback.accept(e); runOptimisationsCallback.accept(e); }, toOptimiseHuffman, "default ");
        }

        return currentSmallestPair.k;
    }

    public long optimise() {
        int block = 0;
        int pass = 0;
        long pos = 0;
        long saved = 0;
        boolean first = true;
        DeflateBlock currentBlock = getFirstBlock();

        while (currentBlock != null) {
            boolean finishPass = true;

            if (currentBlock.getUncompressedData().length > 0) {
                pos += 3;
                final DeflateBlock optimisedBlock = optimiseBlock(currentBlock, pos);
                final long currentSaved = currentBlock.getSizeBits(pos) - optimisedBlock.getSizeBits(pos);

                if ((optimisedBlock != currentBlock) && (currentSaved > 0)) {
                    finishPass = false;
                    pass++;
                    saved += currentSaved;

                    if (PRINT_OPT_FINE) {
                        System.out.println("Pass " + pass + " saved " + currentSaved + " bits in block " + block);
                    }

                    currentBlock.replace(optimisedBlock);
                    currentBlock.discard();
                    currentBlock = optimisedBlock;

                    if (first) {
                        setFirstBlock(optimisedBlock);
                    }
                }

                pos += currentBlock.getSizeBits(pos);
            } else if (!first || (currentBlock.getNext() != null)) {
                final long currentSaved = currentBlock.getSizeBits(pos + 3) + 3;

                if (PRINT_OPT_FINE) {
                    System.out.println("Removed empty block " + block + ", saved " + currentSaved + " bits");
                }

                saved += currentSaved;

                if (first) {
                    setFirstBlock(currentBlock.getNext());
                }

                currentBlock.remove();
            } else {
                // Only one block, and it's empty, but it's technically still a deflate stream.
                pos += currentBlock.getSizeBits(pos + 3) + 3;
            }

            if (finishPass) {
                block++;
                currentBlock = currentBlock.getNext();
                first = false;
                pass = 0;
            }
        }

        // TODO Try other types of blocks
        // TODO Try merging blocks
        return saved;
    }

    public byte[] asBytes() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (!write(baos)) {
            throw new IOException("Could not write deflate stream to bytes");
        }

        return baos.toByteArray();
    }
}
