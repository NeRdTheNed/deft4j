package com.github.NeRdTheNed.deft4j.deflate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.github.NeRdTheNed.deft4j.io.BitInputStream;
import com.github.NeRdTheNed.deft4j.io.BitOutputStream;

public abstract class DeflateBlock {
    static DeflateBlock copy(DeflateBlock old, DeflateBlock newBlock) {
        final DeflateBlock prev = old.getPrevious();

        if (prev != null) {
            newBlock.setPrevious(prev);
        }

        final DeflateBlock next = old.getNext();

        if (next != null) {
            newBlock.setNext(next);
        }

        return newBlock;
    }

    public abstract DeflateBlock copy();
    public abstract long optimise();
    public abstract DeflateBlockType getDeflateBlockType();
    abstract boolean parse(BitInputStream is) throws IOException;
    abstract boolean fromUncompressed(byte[] input);
    abstract boolean write(BitOutputStream os, boolean finalBlock) throws IOException;
    abstract byte[] getUncompressedData();
    public abstract long getSizeBits(long alginment);

    public DeflateBlockUncompressed asUncompressed() {
        if (getDeflateBlockType() != DeflateBlockType.STORED) {
            final DeflateBlockUncompressed newUncom = new DeflateBlockUncompressed(getPrevious());
            newUncom.setNext(getNext());
            newUncom.fromUncompressed(getUncompressedData());
            return newUncom;
        }

        return (DeflateBlockUncompressed) copy();
    }

    private DeflateBlock prevBlock;
    private DeflateBlock nextBlock;

    public DeflateBlock getPrevious() {
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

    DeflateBlock(DeflateBlock prevBlock, DeflateBlock nextBlock) {
        this.prevBlock = prevBlock;
        this.nextBlock = nextBlock;
    }

    DeflateBlock(DeflateBlock prevBlock) {
        this(prevBlock, null);
    }

    public void replace(DeflateBlock replacement) {
        if (prevBlock != null) {
            prevBlock.setNext(replacement);
        }

        if (nextBlock != null) {
            nextBlock.setPrevious(replacement);
        }
    }

    public void replace(DeflateBlock prev, DeflateBlock next) {
        if (prevBlock != null) {
            prevBlock.setNext(prev);
        }

        if (nextBlock != null) {
            nextBlock.setPrevious(next);
        }
    }

    public void insertNext(DeflateBlock block) {
        block.setPrevious(this);

        if (nextBlock != null) {
            block.setNext(nextBlock);
            nextBlock.setPrevious(block);
        }

        setNext(block);
    }

    public void insertPrev(DeflateBlock block) {
        block.setNext(this);

        if (prevBlock != null) {
            block.setPrevious(prevBlock);
            prevBlock.setNext(block);
        }

        setPrevious(block);
    }

    public void remove() {
        if (prevBlock != null) {
            prevBlock.setNext(nextBlock);
        }

        if (nextBlock != null) {
            nextBlock.setPrevious(prevBlock);
        }
    }

    /** Read a slice of decompressed data from the current block or previous blocks, with overlapping backref support. */
    static byte[] readSlice(final long initialBackDist, final long initialSize, final DeflateBlock thisBlock, final byte[] thisBlockData, final long thisBlockSize) {
        assert initialBackDist <= Constants.LZZ_BACKREF_LEN;
        final ByteArrayOutputStream collectedSlice = new ByteArrayOutputStream();
        long backDist = initialBackDist;
        long size = initialSize;

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
                currentBlock = currentBlock.getPrevious();
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
                    collectedSlice.write(currentBlockData, (int) offsetFromStartOfBlock, (int) remainingBytesInBlock);
                    offsetFromStartOfBlock += remainingBytesInBlock;
                    assert currentBlockSize == offsetFromStartOfBlock;
                    currentSize = remainingBytesInBlock;
                } else {
                    assert currentBlockSize == offsetFromStartOfBlock;
                    // Copy overlapping backref
                    // TODO Very inefficient
                    byte[] buffer = collectedSlice.toByteArray();
                    long adjustedBackDist = initialBackDist - buffer.length;

                    for (int currOff = 0; currOff < (currentSize - remainingBytesInBlock); currOff++) {
                        collectedSlice.write(buffer[(int) adjustedBackDist]);
                        adjustedBackDist++;
                        buffer = collectedSlice.toByteArray();
                    }
                }
            } else {
                // Copy wanted size or until the end of block
                currentSize = Math.min(remainingBytesInBlock, size);
                assert currentSize > 0;
                collectedSlice.write(currentBlockData, (int) offsetFromStartOfBlock, (int) currentSize);
            }

            backDist -= currentSize;
            size -= currentSize;
        } while (size > 0);

        return collectedSlice.toByteArray();
    }

    byte[] readSlice(long backDist, long size) {
        final byte[] uncomData = getUncompressedData();
        return readSlice(backDist, size, this, uncomData, uncomData.length);
    }
}
