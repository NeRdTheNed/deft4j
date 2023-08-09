package com.github.NeRdTheNed.deft4j.container;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.NeRdTheNed.deft4j.container.lljzip.RecalculatingZipWriter;
import com.github.NeRdTheNed.deft4j.deflate.DeflateStream;
import com.github.NeRdTheNed.deft4j.util.Util;

import software.coley.lljzip.ZipIO;
import software.coley.lljzip.format.compression.ZipCompressions;
import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.format.write.ZipWriter;
import software.coley.lljzip.util.BufferData;
import software.coley.lljzip.util.ByteDataUtil;

public class ZipFile implements DeflateFilesContainer {
    private Map<LocalFileHeader, DeflateStream> deflateStreamMap;
    private ZipArchive archive;

    public List<GZFile> asGZipFiles() throws IOException {
        final List<GZFile> converted = new ArrayList<>();

        for (final Entry<LocalFileHeader, DeflateStream> entry : deflateStreamMap.entrySet()) {
            final GZFile gz = new GZFile();
            final LocalFileHeader header = entry.getKey();
            gz.setData(entry.getValue());
            gz.setFilename(header.getFileNameAsString());
            converted.add(gz);
        }

        return converted;
    }

    private void syncStreams() throws IOException {
        for (final Entry<LocalFileHeader, DeflateStream> entry : deflateStreamMap.entrySet()) {
            final LocalFileHeader file = entry.getKey();
            final DeflateStream stream = entry.getValue();
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();

            if (!stream.write(bos)) {
                throw new IOException("Deflate stream for LocalFileHeader " + file + " could not be written to bytes");
            }

            final byte[] compressedData = bos.toByteArray();
            file.setFileData(BufferData.wrap(compressedData));
            file.setFileDataLength(compressedData.length);
            file.setCompressedSize(compressedData.length);
            file.setFileData(BufferData.wrap(compressedData));
            final CentralDirectoryFileHeader cenDir = file.getLinkedDirectoryFileHeader();

            if (cenDir != null) {
                cenDir.setCompressedSize(compressedData.length);
            }
        }
    }

    @Override
    public List<DeflateStream> getDeflateStreams() {
        return new ArrayList<>(deflateStreamMap.values());
    }

    @Override
    public boolean write(OutputStream os) throws IOException {
        syncStreams();
        final ZipWriter writer = new RecalculatingZipWriter();
        writer.write(archive, os);
        return true;
    }

    @Override
    public boolean read(InputStream is) throws IOException {
        final byte[] zipBytes = Util.convertInputStreamToBytes(is);
        archive = ZipIO.readStandard(zipBytes);

        // If we failed to detect any files, it's likely a parsing error
        if (archive.getLocalFiles().isEmpty()) {
            System.err.println("No local file headers detected in zip file, may be due to use of data descriptors or Zip64 features");
            return false;
        }

        deflateStreamMap = new HashMap<>();

        for (final LocalFileHeader localFile : archive.getLocalFiles()) {
            // TODO Try compressing uncompressed files
            if (localFile.getCompressionMethod() != ZipCompressions.DEFLATED) {
                continue;
            }

            // Often needed for files with data descriptors
            if ((localFile.getCompressedSize() == 0) && localFile.hasDifferentValuesThanCentralDirectoryHeader()) {
                localFile.adoptLinkedCentralDirectoryValues();
            }

            /*if (localFile.getUncompressedSize() == 0) {
                continue;
            }*/
            final byte[] compressed = ByteDataUtil.toByteArray(localFile.getFileData());
            final ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
            String name = null;

            if (localFile.getFileNameLength() > 0) {
                name = localFile.getFileNameAsString();
            }

            final DeflateStream deflateStream = new DeflateStream(name);

            if (!deflateStream.parse(bis)) {
                System.err.println("Failed to parse stream for file " + name);
                return false;
            }

            deflateStreamMap.put(localFile, deflateStream);
        }

        return true;
    }

    @Override
    public String fileType() {
        return "Zip";
    }

    @Override
    public void close() throws IOException {
        archive.close();
    }

}
