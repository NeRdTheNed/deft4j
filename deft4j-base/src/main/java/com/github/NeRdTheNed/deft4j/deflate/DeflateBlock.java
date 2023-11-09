package com.github.NeRdTheNed.deft4j.deflate;

import java.io.IOException;
import java.util.Arrays;

import com.github.NeRdTheNed.deft4j.io.BitInputStream;
import com.github.NeRdTheNed.deft4j.io.BitOutputStream;

public abstract class DeflateBlock {
    static DeflateBlock copy(DeflateBlock old, DeflateBlock newBlock) {
        final DeflateBlock prev = old.prevBlock;

        if (prev != null) {
            newBlock.prevBlock = prev;
        }

        final DeflateBlock next = old.nextBlock;

        if (next != null) {
            newBlock.nextBlock = next;
        }

        return newBlock;
    }

    public abstract DeflateBlock copy();
    public abstract boolean canMerge(DeflateBlock append);
    public abstract DeflateBlock merge(DeflateBlock append);
    public abstract long optimise();
    public abstract DeflateBlockType getDeflateBlockType();
    abstract boolean parse(BitInputStream is) throws IOException;
    abstract boolean fromUncompressed(byte[] input);
    abstract boolean write(BitOutputStream os, boolean finalBlock) throws IOException;
    abstract byte[] getUncompressedData();
    public abstract long getSizeBits(long alginment);
    public void discard() {
        final DeflateBlock prev = prevBlock;

        if ((prev != null) && (prev.nextBlock == this)) {
            prev.nextBlock = null;
        }

        final DeflateBlock next = nextBlock;

        if ((next != null) && (next.prevBlock == this)) {
            next.prevBlock = null;
        }

        nextBlock = null;
        prevBlock = null;
    }

    public DeflateBlockUncompressed asUncompressed() {
        if (getDeflateBlockType() != DeflateBlockType.STORED) {
            final DeflateBlockUncompressed newUncom = new DeflateBlockUncompressed(prevBlock);
            newUncom.setNext(nextBlock);
            newUncom.fromUncompressed(getUncompressedData());
            return newUncom;
        }

        return (DeflateBlockUncompressed) copy();
    }

    private DeflateBlock prevBlock;
    private DeflateBlock nextBlock;

    DeflateBlock getPrevious() {
        return prevBlock;
    }

    public void setPrevious(DeflateBlock block) {
        prevBlock = block;
    }

    public DeflateBlock getNext() {
        return nextBlock;
    }

    public void setNext(DeflateBlock block) {
        nextBlock = block;
    }

    private DeflateBlock(DeflateBlock prevBlock, DeflateBlock nextBlock) {
        this.prevBlock = prevBlock;
        this.nextBlock = nextBlock;
    }

    DeflateBlock(DeflateBlock prevBlock) {
        this(prevBlock, null);
    }

    public void replace(DeflateBlock replacement) {
        if (prevBlock != null) {
            prevBlock.nextBlock = replacement;
        }

        if (nextBlock != null) {
            nextBlock.prevBlock = replacement;
        }
    }

    public void replace(DeflateBlock prev, DeflateBlock next) {
        if (prevBlock != null) {
            prevBlock.nextBlock = prev;
        }

        if (nextBlock != null) {
            nextBlock.prevBlock = next;
        }
    }

    public void insertNext(DeflateBlock block) {
        block.prevBlock = this;

        if (nextBlock != null) {
            block.nextBlock = nextBlock;
            nextBlock.prevBlock = block;
        }

        nextBlock = block;
    }

    public void insertPrev(DeflateBlock block) {
        block.nextBlock = this;

        if (prevBlock != null) {
            block.prevBlock = prevBlock;
            prevBlock.nextBlock = block;
        }

        prevBlock = block;
    }

    public void remove() {
        if (prevBlock != null) {
            prevBlock.nextBlock = nextBlock;
        }

        if (nextBlock != null) {
            nextBlock.prevBlock = prevBlock;
        }

        discard();
    }

    /** Read a slice of decompressed data from the current block or previous blocks, with overlapping backref support. */
    static byte[] readSlice(long initialBackDist, final long initialSize, final DeflateBlock thisBlock, final byte[] thisBlockData, final long thisBlockSize, final long initialBackDistOffset) {
        assert (initialBackDist <= Constants.LZZ_BACKREF_LEN) && (initialBackDist >= 0);
        assert initialBackDistOffset >= 0;
        initialBackDist += initialBackDistOffset;
        long backDist = initialBackDist;
        long size = initialSize;
        assert size > 0;

        if (backDist < thisBlockSize) {
            final long offsetFromStartOfBlock = thisBlockSize - backDist;
            final long remainingBytesInBlock = thisBlockSize - offsetFromStartOfBlock;

            if (remainingBytesInBlock >= size) {
                return Arrays.copyOfRange(thisBlockData, (int) offsetFromStartOfBlock, (int) (offsetFromStartOfBlock + size));
            }
        }

        final byte[] collectedSlice = new byte[(int) initialSize];
        int currentPosInArr = 0;

        do {
            long currentBackDist = backDist;
            long currentSize = size;
            long currentBlockSize = thisBlockSize;
            DeflateBlock currentBlock = thisBlock;
            byte[] currentBlockData = thisBlockData;

            // Read data from the previous block if needed
            while (currentBackDist > currentBlockSize) {
                currentBackDist -= currentBlockSize;
                currentSize -= currentBlockSize;
                currentBlock = currentBlock.prevBlock;
                assert currentBlock != null;
                currentBlockData = currentBlock.getUncompressedData();
                currentBlockSize = currentBlockData.length;
            }

            long offsetFromStartOfBlock = currentBlockSize - currentBackDist;
            final long remainingBytesInBlock = currentBlockSize - offsetFromStartOfBlock;
            assert remainingBytesInBlock >= 0;

            // Check for overlapping backref
            if (((offsetFromStartOfBlock + currentSize) > currentBlockSize) && (currentBlock == thisBlock)) {
                // Overlapping backref, start by copying any non-overlapping data
                if (remainingBytesInBlock > 0) {
                    System.arraycopy(currentBlockData, (int) offsetFromStartOfBlock, collectedSlice, currentPosInArr, (int) remainingBytesInBlock);
                    currentPosInArr += (int) remainingBytesInBlock;
                    offsetFromStartOfBlock += remainingBytesInBlock;
                    assert currentBlockSize == offsetFromStartOfBlock;
                    currentSize = remainingBytesInBlock;
                } else {
                    assert currentBlockSize == offsetFromStartOfBlock;
                    // Copy overlapping backref
                    // TODO Somewhat inefficient
                    long adjustedBackDist = initialBackDist - currentPosInArr;

                    for (int currOff = 0; currOff < (currentSize - remainingBytesInBlock); currOff++) {
                        collectedSlice[currentPosInArr] = collectedSlice[(int) adjustedBackDist];
                        adjustedBackDist++;
                        currentPosInArr++;
                    }
                }
            } else {
                // Copy wanted size or until the end of block
                currentSize = Math.min(remainingBytesInBlock, size);
                assert currentSize > 0;
                System.arraycopy(currentBlockData, (int) offsetFromStartOfBlock, collectedSlice, currentPosInArr, (int) currentSize);
                currentPosInArr += currentSize;
            }

            backDist -= currentSize;
            size -= currentSize;
        } while (size > 0);

        return collectedSlice;
    }

    /** Read a slice of decompressed data from the current block or previous blocks, with overlapping backref support. */
    static byte[] readSlice(final long initialBackDist, final long initialSize, final DeflateBlock thisBlock, final byte[] thisBlockData, final long thisBlockSize) {
        return readSlice(initialBackDist, initialSize, thisBlock, thisBlockData, thisBlockSize, 0);
    }

    byte[] readSlice(long backDist, long size, long offset) {
        final byte[] uncomData = getUncompressedData();
        return readSlice(backDist, size, this, uncomData, uncomData.length, offset);
    }

    byte[] readSlice(long backDist, long size) {
        return readSlice(backDist, size, 0);
    }
}
