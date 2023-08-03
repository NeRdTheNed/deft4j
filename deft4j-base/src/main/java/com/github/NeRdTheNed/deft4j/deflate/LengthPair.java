package com.github.NeRdTheNed.deft4j.deflate;

class LengthPair {
    public final long baseLen;
    public final long ebits;

    LengthPair(long baseLen, long ebits) {
        this.baseLen = baseLen;
        this.ebits = ebits;
    }
}
