package com.github.NeRdTheNed.deft4j.cmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "deft4j", version = "deft4j v1.0.0-beta-11", subcommands = { Optimise.class, OptimiseFolder.class, ConvertToGZip.class })
public class Main {
    public static void main(String[] args) {
        final int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
