package com.github.NeRdTheNed.deft4j.util;

import java.util.Objects;

public class Pair<K, V> {
    @Override
    public int hashCode() {
        return Objects.hash(k, v);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Pair)) {
            return false;
        }

        final Pair other = (Pair) obj;
        return Objects.equals(k, other.k) && Objects.equals(v, other.v);
    }

    public K k;
    public V v;
    public Pair(K k, V v) {
        this.k = k;
        this.v = v;
    }
}
