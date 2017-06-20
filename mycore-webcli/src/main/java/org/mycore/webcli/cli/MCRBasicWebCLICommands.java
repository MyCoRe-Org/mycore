package org.mycore.webcli.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.cli.MCRCommandLineInterface;
import org.mycore.frontend.cli.MCRCommandStatistics;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.webcli.container.MCRWebCLIContainer;

@MCRCommandGroup(name = "Basic commands")
public class MCRBasicWebCLICommands {
    @MCRCommand(syntax = "process {0}", help = "Execute the commands listed in the text file {0}.")
    public static List<String> readCommandsFile(String file) throws IOException, FileNotFoundException {
        return MCRCommandLineInterface.readCommandsFile(file);
    }

    @MCRCommand(syntax = "show command statistics",
        help = "Show statistics on number of commands processed and execution time needed per command")
    public static void showCommandStatistics() {
        MCRCommandStatistics.showCommandStatistics();
    }

    @MCRCommand(syntax = "cancel on error", help = "Cancel execution of further commands in case of error")
    public static void cancelonError() {
        setContinueIfOneFails(false);
        MCRCommandLineInterface.cancelOnError();
    }

    @MCRCommand(syntax = "skip on error", help = "Skip execution of failed command in case of error")
    public static void skipOnError() {
        setContinueIfOneFails(true);
        MCRCommandLineInterface.skipOnError();
    }

    private static void setContinueIfOneFails(boolean value) {
        Object sessionValue;
        synchronized (MCRSessionMgr.getCurrentSession()) {
            sessionValue = MCRSessionMgr.getCurrentSession().get("MCRWebCLI");
            ((MCRWebCLIContainer) sessionValue).setContinueIfOneFails(value, true);
        }
    }
}
