package com.github.NeRdTheNed.deft4j.deflate;

import java.util.Arrays;
import java.util.Objects;

public class LitLen {
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + Arrays.hashCode(decodedVal);
        return (prime * result) + Objects.hash(dist, edgecase, litlen);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof LitLen)) {
            return false;
        }

        final LitLen other = (LitLen) obj;
        return Arrays.equals(decodedVal, other.decodedVal) && (dist == other.dist) && (edgecase == other.edgecase) && (litlen == other.litlen);
    }

    public final long dist;
    public final long litlen;
    public final boolean edgecase;

    public byte[] decodedVal;

    public LitLen(long dist, long litlen, boolean edgecase) {
        this.dist = dist;
        this.litlen = litlen;
        this.edgecase = edgecase;
    }

    public LitLen(long dist, long litlen) {
        this(dist, litlen, false);
    }

    public LitLen(long litlen) {
        this(0, litlen);
    }

    public LitLen copy() {
        final LitLen newLit = new LitLen(dist, litlen, edgecase);

        if (decodedVal != null) {
            //newLit.decodedVal = new byte[decodedVal.length];
            //System.arraycopy(decodedVal, 0, newLit.decodedVal, 0, decodedVal.length);
            newLit.decodedVal = decodedVal;
        }

        return newLit;
    }

    @Override
    public String toString() {
        return "LitLen [litlen=" + litlen + ", dist=" + dist + ", edgecase=" + edgecase + ", decodedVal=" + Arrays.toString(decodedVal) + "]";
    }
}
