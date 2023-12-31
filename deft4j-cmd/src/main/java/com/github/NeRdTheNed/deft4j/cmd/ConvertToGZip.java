package com.github.NeRdTheNed.deft4j.cmd;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;

import com.github.NeRdTheNed.deft4j.container.ContainerUtil;
import com.github.NeRdTheNed.deft4j.container.DeflateFilesContainer;
import com.github.NeRdTheNed.deft4j.container.GZFile;
import com.github.NeRdTheNed.deft4j.container.ToGZipConvertible;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "convert-to-gzip", aliases = { "convert-png", "convert-zip" }, description = "Converts a given file to GZip files")
class ConvertToGZip implements Callable<Integer> {

    @Parameters(index = "0", description = "The input file")
    private Path inputFile;

    @Parameters(index = "1", description = "The directory to output files to")
    private Path outputFile;

    @Override
    public Integer call() throws Exception {
        if (!Files.isRegularFile(inputFile)) {
            System.err.println("Error: Input file does not exist");
            return 1;
        }

        if (!Files.isDirectory(outputFile)) {
            System.err.println("Error: Output file is not a directory");
            return 1;
        }

        boolean noErr = false;
        List<GZFile> converted = null;

        try
            (final InputStream is = new BufferedInputStream(Files.newInputStream(inputFile));
                    final DeflateFilesContainer container = ContainerUtil.getContainerForPath(inputFile)) {
            if (!container.read(is)) {
                System.err.println("Failed to read input file");
            }

            System.out.println("Read input file " + inputFile);

            if (container instanceof ToGZipConvertible) {
                converted = ((ToGZipConvertible) container).asGZipFiles();
                System.out.println("Converted to GZip, writing files...");
            } else {
                System.out.println("Could not convert input file " + inputFile + " to GZip");
            }
        } catch (final IOException e) {
            System.err.println("IOException thrown when reading file " + inputFile);
            e.printStackTrace();
        }

        if (converted != null) {
            final Collection<String> seenNames = new HashSet<>();
            int defaultNameCount = 0;

            for (final GZFile file : converted) {
                String fileName = file.getFilename();

                while ((fileName == null) || fileName.isEmpty() || seenNames.contains(fileName)) {
                    if (fileName == null) {
                        fileName = "Unknown-";
                    }

                    fileName = fileName + defaultNameCount;
                    defaultNameCount++;
                }

                seenNames.add(fileName);
                System.out.println("File name " + fileName);
                final Path choosen = outputFile.resolve(fileName + ".gz");
                Files.createDirectories(choosen.getParent());
                final Path output = Files.createFile(choosen);

                try
                    (final OutputStream os = Files.newOutputStream(output)) {
                    file.write(os);
                }

                System.out.println("Wrote " + output);
                file.close();
            }

            noErr = true;
        }

        return noErr ? CommandLine.ExitCode.OK : 1;
    }

}
