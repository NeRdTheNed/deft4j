package com.github.NeRdTheNed.deft4j.cmd;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.github.NeRdTheNed.deft4j.container.GZFile;
import com.github.NeRdTheNed.deft4j.deflate.DeflateStream;
import com.github.NeRdTheNed.deft4j.util.Util;
import com.github.NeRdTheNed.deft4j.util.compression.CompressionUtil;
import com.github.NeRdTheNed.deft4j.util.compression.CompressionUtil.Strategy;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "gzip-compress", mixinStandardHelpOptions = true, description = "Compress file to GZip")
class GZipCompress implements Callable<Integer> {
    public enum CompressMode {
        CHEAP,
        ZOPFLI,
        ZOPFLI_EXTENSIVE,
        ZOPFLI_VERY_EXTENSIVE
    }

    @Parameters(index = "0", description = "The file to compress")
    private Path inputFile;

    @Parameters(index = "1", description = "The compressed file")
    private Path outputFile;

    @Option(names = { "--compress-mode", "--mode", "-m" }, defaultValue = "CHEAP", description = "Enable various levels of compression. Default: ${DEFAULT-VALUE}. Valid values: ${COMPLETION-CANDIDATES}")
    private CompressMode recompressMode;

    @Option(names = { "--zopfli-iter", "--iter", "-z" }, defaultValue = "20", description = "Zopfli iterations")
    int iter = 20;

    @Option(names = { "--keep-name", "-n" }, negatable = true, defaultValue = "true", fallbackValue = "true", description = "Write filename to GZip header")
    boolean name = true;

    @Option(names = { "--keep-mtime", "-t" }, negatable = true, defaultValue = "true", fallbackValue = "true", description = "Write last modified time to GZip header")
    boolean mtime = true;

    @Option(names = { "--keep-os", "-o" }, negatable = true, defaultValue = "true", fallbackValue = "true", description = "Write current OS to GZip header")
    boolean os = true;

    @Option(names = { "--default-os", "-O" }, defaultValue = "255", description = "Used to override the default OS value if keep-os is false, or the current OS can't be detected. Defaults to 255 (unknown). Some programs default to 0 (MS-DOS / FAT filesystem).")
    int defaultOS = 255;

    @Option(names = { "--text", "-T" }, defaultValue = "false", description = "File is ASCII text")
    boolean text;

    private static CompressionUtil getComp(CompressMode mode, int iter) {
        final boolean zopfli = mode.ordinal() >= CompressMode.ZOPFLI.ordinal();
        final Strategy strat = mode.ordinal() >= CompressMode.ZOPFLI_EXTENSIVE.ordinal() ? Strategy.EXTENSIVE : Strategy.MULTI_CHEAP;
        return new CompressionUtil(true, true, mode.ordinal() >= CompressMode.ZOPFLI_VERY_EXTENSIVE.ordinal(), zopfli, iter, strat, true, true);
    }

    private static int getOS(int fallback) {
        // TODO Modern versions of ZLib use current APPNOTE.TXT operating system mappings, possibly use them instead?
        try {
            final String osName = System.getProperty("os.name");

            if (osName != null) {
                // Windows NTFS
                if (osName.startsWith("Windows")) {
                    return 11;
                }

                // Macintosh
                if (osName.startsWith("Mac")) {
                    return 7;
                }

                // Unix
                if (osName.startsWith("Linux") || osName.startsWith("LINUX")) {
                    return 3;
                }
            }
        } catch (final Exception e) {
            // Ignored
        }

        // Unknown
        return fallback;
    }

    @Override
    public Integer call() throws Exception {
        if (!Files.isRegularFile(inputFile)) {
            System.err.println("Error: Input file does not exist");
            return 1;
        }

        if (Files.isRegularFile(outputFile)) {
            System.err.println("Error: Output file already exists");
            return 1;
        }

        if (Files.isDirectory(outputFile)) {
            System.err.println("Error: Output file is a directory");
            return 1;
        }

        final CompressionUtil compUtil = getComp(recompressMode, iter);
        final byte[] uncompressed;

        try
            (final InputStream is = new BufferedInputStream(Files.newInputStream(inputFile))) {
            uncompressed = Util.convertInputStreamToBytes(is);
        }

        final byte[] compresed = compUtil.compress(uncompressed, true);
        final DeflateStream stream = new DeflateStream();
        stream.parse(compresed);
        final boolean didOpt;

        try
            (GZFile out = new GZFile()) {
            out.setData(stream);

            if (name) {
                out.setFilename(inputFile.getFileName().toString());
            }

            if (mtime) {
                out.setMTime(Util.getFileModifiedTimeSecondsOrZero(inputFile));
            }

            out.setOS(os ? getOS(defaultOS) : defaultOS);
            out.setText(text);

            try
                (final OutputStream fos = new BufferedOutputStream(Files.newOutputStream(outputFile))) {
                didOpt = out.write(fos);
            }
        }

        if (!didOpt) {
            System.err.println("Failed to compress " + inputFile);
        }

        return didOpt ? CommandLine.ExitCode.OK : 1;
    }
}
