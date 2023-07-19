package com.github.NeRdTheNed.deft4j.deflate;

public class LengthPair {
    public final long baseLen;
    public final long ebits;

    public LengthPair(long baseLen, long ebits) {
        this.baseLen = baseLen;
        this.ebits = ebits;
    }
}
