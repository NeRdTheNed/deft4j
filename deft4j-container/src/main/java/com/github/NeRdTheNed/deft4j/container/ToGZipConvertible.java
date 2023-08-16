package com.github.NeRdTheNed.deft4j.container;

import java.io.IOException;
import java.util.List;

public interface ToGZipConvertible {
    List<GZFile> asGZipFiles() throws IOException;
}
