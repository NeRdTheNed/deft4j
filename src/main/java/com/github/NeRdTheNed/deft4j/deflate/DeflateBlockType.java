package com.github.NeRdTheNed.deft4j.deflate;

public enum DeflateBlockType {
    STORED,
    FIXED,
    DYNAMIC;

    public static DeflateBlockType fromInt(int i) {
        return DeflateBlockType.values()[i];
    }
}
