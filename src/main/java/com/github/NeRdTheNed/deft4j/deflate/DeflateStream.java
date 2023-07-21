package com.github.NeRdTheNed.deft4j.deflate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.github.NeRdTheNed.deft4j.Deft;
import com.github.NeRdTheNed.deft4j.io.BitInputStream;
import com.github.NeRdTheNed.deft4j.io.BitOutputStream;

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

    private static DeflateBlockHuffman optimiseBlockRecodePre(DeflateBlockHuffman block, boolean ohh) {
        if (block.getDeflateBlockType() != DeflateBlockType.DYNAMIC) {
            return null;
        }

        final DeflateBlockHuffman optimised = (DeflateBlockHuffman) block.copy();
        optimised.rewriteHeader(ohh);
        optimised.optimise();
        return optimised;
    }

    private static DeflateBlockHuffman optimiseBlockRecodePost(DeflateBlockHuffman block, boolean ohh) {
        if (block.getDeflateBlockType() != DeflateBlockType.DYNAMIC) {
            return null;
        }

        final DeflateBlockHuffman optimised = (DeflateBlockHuffman) block.copy();
        optimised.optimise();
        optimised.rewriteHeader(ohh);
        optimised.optimise();
        return optimised;
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

        final Map<DeflateBlock, String> candidates = new HashMap<>();
        // Standard
        final DeflateBlock optimised = optimiseBlockNormal(toOptimise);
        candidates.put(optimised, "optimised");

        if (toOptimise.getDeflateBlockType() != DeflateBlockType.STORED) {
            // Uncompressed
            final DeflateBlockUncompressed uncompressed = toOptimise.asUncompressed();
            candidates.put(uncompressed, "uncompressed");
        }

        if (toOptimise.getDeflateBlockType() == DeflateBlockType.DYNAMIC) {
            final DeflateBlockHuffman toOptimiseHuffman = (DeflateBlockHuffman) toOptimise;
            final DeflateBlockHuffman optimisedHuffman = (DeflateBlockHuffman) optimised;
            // Recoded pre optimisations
            final DeflateBlockHuffman pre = optimiseBlockRecodePre(toOptimiseHuffman, false);
            candidates.put(pre, "recoded optimised");
            // Recoded pre optimisations ohh
            final DeflateBlockHuffman preOhh = optimiseBlockRecodePre(toOptimiseHuffman, true);
            candidates.put(preOhh, "recoded optimised ohh");
            // Recoded post optimisations
            final DeflateBlockHuffman post = optimiseBlockRecodePost(toOptimiseHuffman, false);
            candidates.put(post, "optimised recoded");
            // Recoded post optimisations ohh
            final DeflateBlockHuffman postOhh = optimiseBlockRecodePost(toOptimiseHuffman, true);
            candidates.put(postOhh, "optimised recoded ohh");

            for (final DeflateBlockHuffman toFixed : new DeflateBlockHuffman[] { toOptimiseHuffman, optimisedHuffman, pre, post, preOhh, postOhh }) {
                // Fixed huffman block
                final String name = candidates.getOrDefault(toFixed, "default") + " fixed huffman";
                final DeflateBlockHuffman fixed = toFixedHuffman(toFixed);
                fixed.optimise();
                candidates.put(fixed, name);
            }
        }

        DeflateBlock currentSmallest = toOptimise;
        long currentSizeBits = currentSmallest.getSizeBits(position);

        for (final Entry<DeflateBlock, String> candidateEntry : candidates.entrySet()) {
            if (PRINT_OPT_FINER) {
                System.out.println("Trying " + candidateEntry.getValue());
            }

            final DeflateBlock candidate = candidateEntry.getKey();
            final long newSizeBits = candidate.getSizeBits(position);

            if (newSizeBits < currentSizeBits) {
                if (PRINT_OPT_FINE) {
                    System.out.println("Candidate " + candidateEntry.getValue() + " saved " + (currentSizeBits - newSizeBits) + " bits");
                }

                currentSmallest = candidate;
                currentSizeBits = newSizeBits;
            }
        }

        return currentSmallest;
    }

    public long optimise() {
        int block = 0;
        long pos = 0;
        long saved = 0;
        boolean first = true;
        DeflateBlock currentBlock = getFirstBlock();

        while (currentBlock != null) {
            if (currentBlock.getUncompressedData().length > 0) {
                pos += 3;
                final DeflateBlock optimisedBlock = optimiseBlock(currentBlock, pos);
                final long currentSaved = currentBlock.getSizeBits(pos) - optimisedBlock.getSizeBits(pos);

                if ((optimisedBlock != currentBlock) && (currentSaved > 0)) {
                    saved += currentSaved;

                    if (PRINT_OPT_FINE) {
                        System.out.println("Saved " + currentSaved + " bits in block " + block);
                    }

                    currentBlock.replace(optimisedBlock);
                    currentBlock = optimisedBlock;

                    if (first) {
                        setFirstBlock(optimisedBlock);
                    }
                }

                pos += currentBlock.getSizeBits(pos);
            } else {
                final long currentSaved = currentBlock.getSizeBits(pos + 3) + 3;

                if (PRINT_OPT_FINE) {
                    System.out.println("Removed empty block " + block + ", saved " + currentSaved + " bits");
                }

                saved += currentSaved;
                currentBlock.remove();

                if (first) {
                    setFirstBlock(currentBlock.getNext());
                }
            }

            block++;
            currentBlock = currentBlock.getNext();
            first = false;
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
