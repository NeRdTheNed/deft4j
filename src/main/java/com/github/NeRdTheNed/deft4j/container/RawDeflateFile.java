package com.github.NeRdTheNed.deft4j.container;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.github.NeRdTheNed.deft4j.deflate.DeflateStream;

public class RawDeflateFile implements DeflateFilesContainer {
    DeflateStream deflateStream;

    @Override
    public List<DeflateStream> getDeflateStreams() {
        final ArrayList<DeflateStream> fileList = new ArrayList<>();
        fileList.add(deflateStream);
        return fileList;
    }

    @Override
    public boolean write(OutputStream os) throws IOException {
        return deflateStream.write(os);
    }

    @Override
    public boolean read(InputStream is) throws IOException {
        deflateStream = new DeflateStream();
        return deflateStream.parse(is);
    }

    @Override
    public String fileType() {
        return "Raw deflate stream";
    }
}
