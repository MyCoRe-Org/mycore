package org.mycore.webcli.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.mycore.common.MCRClassTools;
import org.mycore.frontend.cli.MCRCommandLineInterface;
import org.mycore.frontend.cli.MCRCommandStatistics;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.webcli.cli.command.MCRWebCLICommand;

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

    /**
     * Checks cmdPath for jar files and loads 
     * commands into the command pool.
     * 
     * @param path to the jar files
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws ClassNotFoundException 
     * @throws IOException 
     * @throws MalformedURLException 
     */
    @MCRCommand(help = "add all commands in jar file {0} to system", syntax = "add commands {0}", order = 1000)
    public static void addCommand(String path) throws MalformedURLException, ClassNotFoundException,
        InstantiationException, IllegalAccessException, IOException {
        File file = new File(path);

        if (file.isDirectory()) {
            File[] jarFiles = MCRJarTools.listJarFiles(file);
            for (File jarFile : jarFiles) {
                addCommandFromJar(jarFile);
            }
        } else {
            addCommandFromJar(file);
        }
    }

    private static void addCommandFromJar(File file) throws IOException, MalformedURLException, ClassNotFoundException,
        InstantiationException, IllegalAccessException {
        ArrayList<String> classesFromJar = MCRJarTools.getClassesFromJar(file);

        for (String className : classesFromJar) {
            Object cmd = MCRClassTools.loadClassFromURL(file, className);
            if (cmd instanceof MCRWebCLICommand) {
                Logger.getLogger(MCRBasicWebCLICommands.class).info("Adding command to pool: " + cmd);
                MCRCommandPool.instance().addCommand((MCRWebCLICommand) cmd);
            }
        }
    }
}
