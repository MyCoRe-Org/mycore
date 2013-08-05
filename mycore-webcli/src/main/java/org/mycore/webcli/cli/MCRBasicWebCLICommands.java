package org.mycore.webcli.cli;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.mycore.frontend.cli.MCRCommandLineInterface;
import org.mycore.frontend.cli.MCRCommandStatistics;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

@MCRCommandGroup(name = "Basic commands")
public class MCRBasicWebCLICommands {
    @MCRCommand(syntax = "process {0}", help = "Execute the commands listed in the text file {0}.")
    public static void readCommandsFile(String file) throws IOException, FileNotFoundException {
        MCRCommandLineInterface.readCommandsFile(file);
    }

    @MCRCommand(syntax = "show command statistics", help = "Show statistics on number of commands processed and execution time needed per command")
    public static void showCommandStatistics() {
        MCRCommandStatistics.showCommandStatistics();
    }

    @MCRCommand(syntax = "cancel on error", help = "Cancel execution of further commands in case of error")
    public static void cancelonError() {
        MCRCommandLineInterface.cancelOnError();
    }

    @MCRCommand(syntax = "skip on error", help = "Skip execution of failed command in case of error")
    public static void skipOnError() {
        MCRCommandLineInterface.skipOnError();
    }
}
