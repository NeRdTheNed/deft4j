package com.github.NeRdTheNed.deft4j.container.lljzip;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.github.NeRdTheNed.deft4j.util.Util;

import software.coley.lljzip.format.ZipPatterns;
import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.EndOfCentralDirectory;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.format.write.ZipWriter;
import software.coley.lljzip.util.ByteDataUtil;

public class RecalculatingZipWriter implements ZipWriter {

    @Override
    public void write(@Nonnull ZipArchive archive, @Nonnull OutputStream os) throws IOException {
        final Map<Long, Long> originalOffsetToNewOffset = new HashMap<>();
        long offset = 0;

        // Write local file headers.
        for (final LocalFileHeader fileHeader : archive.getLocalFiles()) {
            final CentralDirectoryFileHeader cenDir = fileHeader.getLinkedDirectoryFileHeader();
            long compressedSize = fileHeader.getCompressedSize();
            long uncompressedSize = fileHeader.getUncompressedSize();
            int crc32 = fileHeader.getCrc32();
            final int fileNameLength = fileHeader.getFileNameLength();
            final int extraFieldLength = fileHeader.getExtraFieldLength();

            if (cenDir != null) {
                compressedSize = cenDir.getCompressedSize();
                uncompressedSize = cenDir.getUncompressedSize();
                crc32 = cenDir.getCrc32();
            }

            Util.writeIntLE(os, ZipPatterns.LOCAL_FILE_HEADER_QUAD);
            Util.writeShortLE(os, fileHeader.getVersionNeededToExtract());
            Util.writeShortLE(os, fileHeader.getGeneralPurposeBitFlag());
            Util.writeShortLE(os, fileHeader.getCompressionMethod());
            Util.writeShortLE(os, fileHeader.getLastModFileTime());
            Util.writeShortLE(os, fileHeader.getLastModFileDate());
            Util.writeIntLE(os, crc32);
            Util.writeIntLE(os, (int) compressedSize);
            Util.writeIntLE(os, (int) uncompressedSize);
            Util.writeShortLE(os, fileNameLength);
            Util.writeShortLE(os, extraFieldLength);
            os.write(ByteDataUtil.toByteArray(fileHeader.getFileName()));
            os.write(ByteDataUtil.toByteArray(fileHeader.getExtraField()));
            os.write(ByteDataUtil.toByteArray(fileHeader.getFileData()));

            if (cenDir != null) {
                originalOffsetToNewOffset.put(cenDir.getRelativeOffsetOfLocalHeader(), offset);
            } else if (fileHeader.hasOffset()) {
                originalOffsetToNewOffset.put(fileHeader.offset(), offset);
            }

            offset += 30 + fileNameLength + extraFieldLength + compressedSize;
        }

        final long startCentral = offset;
        int centralEntries = 0;

        // Write central directory file headers.
        for (final CentralDirectoryFileHeader directory : archive.getCentralDirectories()) {
            long relativeOffset = directory.getRelativeOffsetOfLocalHeader();

            if (!originalOffsetToNewOffset.containsKey(relativeOffset)) {
                throw new IOException("Error writing CentralDirectoryFileHeader " + directory + ", could not find old offset");
            }

            relativeOffset = originalOffsetToNewOffset.get(relativeOffset);
            long compressedSize = directory.getCompressedSize();
            long uncompressedSize = directory.getUncompressedSize();
            final int fileNameLength = directory.getFileNameLength();
            final int extraFieldLength = directory.getExtraFieldLength();
            final int fileCommentLength = directory.getFileCommentLength();
            final LocalFileHeader linked = directory.getLinkedFileHeader();

            if (linked != null) {
                compressedSize = linked.getCompressedSize();
                uncompressedSize = linked.getUncompressedSize();
            }

            Util.writeIntLE(os, ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER_QUAD);
            Util.writeShortLE(os, directory.getVersionMadeBy());
            Util.writeShortLE(os, directory.getVersionNeededToExtract());
            Util.writeShortLE(os, directory.getGeneralPurposeBitFlag());
            Util.writeShortLE(os, directory.getCompressionMethod());
            Util.writeShortLE(os, directory.getLastModFileTime());
            Util.writeShortLE(os, directory.getLastModFileDate());
            Util.writeIntLE(os, directory.getCrc32());
            Util.writeIntLE(os, (int) compressedSize);
            Util.writeIntLE(os, (int) uncompressedSize);
            Util.writeShortLE(os, fileNameLength);
            Util.writeShortLE(os, extraFieldLength);
            Util.writeShortLE(os, fileCommentLength);
            Util.writeShortLE(os, directory.getDiskNumberStart());
            Util.writeShortLE(os, directory.getInternalFileAttributes());
            Util.writeIntLE(os, directory.getExternalFileAttributes());
            Util.writeIntLE(os, (int) relativeOffset);
            os.write(ByteDataUtil.toByteArray(directory.getFileName()));
            os.write(ByteDataUtil.toByteArray(directory.getExtraField()));
            os.write(ByteDataUtil.toByteArray(directory.getFileComment()));
            offset += 46 + fileNameLength + extraFieldLength + fileCommentLength;
            centralEntries++;
        }

        // Write end of central directory record.
        final EndOfCentralDirectory end = archive.getEnd();
        int diskNumber = 0;
        int startDisk = 0;
        final long centralSize = offset - startCentral;
        byte[] zipComment = {};

        if (end != null) {
            diskNumber = end.getDiskNumber();
            startDisk = end.getCentralDirectoryStartDisk();
            zipComment = ByteDataUtil.toByteArray(end.getZipComment());
        }

        Util.writeIntLE(os, ZipPatterns.END_OF_CENTRAL_DIRECTORY_QUAD);
        Util.writeShortLE(os, diskNumber);
        Util.writeShortLE(os, startDisk);
        Util.writeShortLE(os, centralEntries);
        Util.writeShortLE(os, centralEntries);
        Util.writeIntLE(os, (int) centralSize);
        Util.writeIntLE(os, (int) startCentral);
        Util.writeShortLE(os, zipComment.length);
        os.write(zipComment);
    }

}
