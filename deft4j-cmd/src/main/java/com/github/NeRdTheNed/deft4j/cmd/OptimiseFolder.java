package com.github.NeRdTheNed.deft4j.cmd;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.NeRdTheNed.deft4j.cmd.CMDUtil.RecompressMode;
import com.github.NeRdTheNed.deft4j.container.ContainerUtil;
import com.github.NeRdTheNed.deft4j.container.DeflateFilesContainer;
import com.github.NeRdTheNed.deft4j.container.RawDeflateFile;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "optimiseFolder", mixinStandardHelpOptions = true, description = "In-place whole folder deflate stream optimiser")
class OptimiseFolder implements Callable<Integer> {
    @Parameters(index = "0", description = "The folder to optimise")
    private Path inputFolder;

    @Option(names = { "--recompress-mode", "--mode", "-m" }, defaultValue = "NONE", description = "Enable various levels of recompression. Default: ${DEFAULT-VALUE}. Valid values: ${COMPLETION-CANDIDATES}")
    private RecompressMode recompressMode;

    @Override
    public Integer call() throws Exception {
        final CMDUtil deft = new CMDUtil(recompressMode);
        boolean didOpt = true;

        try
            (final Stream<Path> files = Files.isDirectory(inputFolder) ? Files.walk(inputFolder) : Stream.of(inputFolder)) {
            for (final Path overwriteInput : files.collect(Collectors.toList())) {
                DeflateFilesContainer detectedFormat;

                if (!Files.isRegularFile(overwriteInput) || ((detectedFormat = ContainerUtil.getContainerForPath(overwriteInput)) == null) || (detectedFormat instanceof RawDeflateFile)) {
                    continue;
                }

                try {
                    if (!deft.optimiseFile(overwriteInput, overwriteInput, null, false)) {
                        didOpt = false;
                    }
                } catch (final Exception e) {
                    System.err.println("Error when optimising file " + overwriteInput);
                    e.printStackTrace();
                    didOpt = false;
                }
            }
        }

        if (!didOpt) {
            System.err.println("Failed to optimise " + inputFolder);
        }

        return didOpt ? CommandLine.ExitCode.OK : 1;
    }
}
