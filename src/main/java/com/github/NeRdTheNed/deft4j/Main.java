package com.github.NeRdTheNed.deft4j;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.github.NeRdTheNed.deft4j.Deft.RecompressMode;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "deft4j", mixinStandardHelpOptions = true, version = "deft4j v1.0.0",
         description = "Deflate stream optimiser")
public class Main implements Callable<Integer> {
    @Parameters(index = "0", description = "The file to optimise")
    private Path inputFile;

    @Parameters(index = "1", description = "The optimised file")
    private Path outputFile;

    @Option(names = { "--format", "-f" }, description = "File format")
    private String format;

    @Option(names = { "--raw", "-r" }, defaultValue = "false", description = "Ignore file format, treat input as a raw deflate stream")
    private boolean raw;

    @Option(names = { "--recompress-mode", "--mode", "-m" }, defaultValue = "NONE", description = "Enable various levels of recompression. Valid values: ${COMPLETION-CANDIDATES}")
    private RecompressMode recompressMode;

    @Override
    public Integer call() throws Exception {
        final Deft deft = new Deft(recompressMode);
        boolean didOpt = false;

        try {
            didOpt = deft.optimiseFile(inputFile, outputFile, format, raw);
        } catch (final Exception e) {
            System.err.println("Error when optimising file " + inputFile);
            e.printStackTrace();
            throw new Exception(e);
        }

        if (!didOpt) {
            System.err.println("Failed to optimise " + inputFile);
        }

        return didOpt ? CommandLine.ExitCode.OK : 1;
    }

    public static void main(String[] args) {
        final int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
