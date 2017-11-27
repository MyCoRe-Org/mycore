/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.frontend.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.xml.sax.SAXParseException;

/**
 * This class contains the basic commands for MyCoRe Command Line and WebCLI.
 *
 * @author Robert Stephan
 */

@MCRCommandGroup(name = "Basic Commands")
public class MCRBasicCommands {
    private static Logger LOGGER = LogManager.getLogger(MCRBasicCommands.class);

    /**
     * Shows a list of commands understood by the command line interface and
     * shows their input syntax. This method implements the "help" command
     */
    @MCRCommand(syntax = "help", help = "List all possible commands.", order = 20)
    public static void listKnownCommands() {
        MCRCommandLineInterface.output("The following commands can be used:");
        MCRCommandLineInterface.output("");

        MCRCommandManager
            .getKnownCommands().entrySet().stream().forEach(e -> {
                outputGroup(e.getKey());
                e.getValue().forEach(org.mycore.frontend.cli.MCRCommand::outputHelp);
            });
    }

    /**
     * Shows the help text for one or more commands.
     *
     * @param pattern
     *            the command, or a fragment of it
     */
    @MCRCommand(syntax = "help {0}", help = "Show the help text for the commands beginning with {0}.", order = 10)
    public static void listKnownCommandsBeginningWithPrefix(String pattern) {
        TreeMap<String, List<org.mycore.frontend.cli.MCRCommand>> matchingCommands = MCRCommandManager
            .getKnownCommands().entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().stream()
                .filter(cmd -> cmd.getSyntax().contains(pattern) || cmd.getHelpText().contains(pattern))
                .collect(Collectors.toList()), (k, v) -> k, TreeMap::new));

        matchingCommands.entrySet().removeIf(e -> e.getValue().isEmpty());

        if (matchingCommands.isEmpty()) {
            MCRCommandLineInterface.output("Unknown command:" + pattern);
        } else {
            MCRCommandLineInterface.output("");

            matchingCommands.forEach((grp, cmds) -> {
                outputGroup(grp);
                cmds.forEach(org.mycore.frontend.cli.MCRCommand::outputHelp);
            });
        }
    }

    private static void outputGroup(String group) {
        MCRCommandLineInterface.output(group);
        MCRCommandLineInterface.output(new String(new char[70]).replace("\0", "-"));
        MCRCommandLineInterface.output("");
    }

    @MCRCommand(syntax = "process {0}", help = "Execute the commands listed in the text file {0}.", order = 30)
    public static List<String> readCommandsFile(String file) throws IOException {
        return MCRCommandLineInterface.readCommandsFile(file);
    }

    @MCRCommand(syntax = "exit", help = "Stop and exit the commandline tool.", order = 40)
    public static void exit() {
        MCRCommandLineInterface.exit();
    }

    @MCRCommand(syntax = "quit", help = "Stop and exit the commandline tool.", order = 50)
    public static void quit() {
        MCRCommandLineInterface.exit();
    }

    @MCRCommand(syntax = "! {0}",
        help = "Execute the shell command {0}, for example '! ls' or '! cmd /c dir'",
        order = 60)
    public static void executeShellCommand(String command) throws Exception {
        MCRCommandLineInterface.executeShellCommand(command);
    }

    @MCRCommand(syntax = "show file {0}", help = "Show contents of local file {0}", order = 70)
    public static void show(String file) throws Exception {
        MCRCommandLineInterface.show(file);
    }

    @MCRCommand(syntax = "whoami", help = "Print the current user.", order = 80)
    public static void whoami() {
        MCRCommandLineInterface.whoami();
    }

    @MCRCommand(syntax = "show command statistics",
        help = "Show statistics on number of commands processed and execution time needed per command",
        order = 90)
    public static void showCommandStatistics() {
        MCRCommandStatistics.showCommandStatistics();
    }

    @MCRCommand(syntax = "cancel on error", help = "Cancel execution of further commands in case of error", order = 100)
    public static void cancelonError() {
        MCRCommandLineInterface.cancelOnError();
    }

    @MCRCommand(syntax = "skip on error", help = "Skip execution of failed command in case of error", order = 110)
    public static void skipOnError() {
        MCRCommandLineInterface.skipOnError();
    }

    @MCRCommand(syntax = "get uri {0} to file {1}",
        help = "Get XML content from URI {0} and save it to a local file {1}",
        order = 120)
    public static void getURI(String uri, String file) throws Exception {
        MCRCommandLineInterface.getURI(uri, file);
    }

    @MCRCommand(syntax = "create configuration directory",
        help = "Creates the MCRConfiguration directory if it does not exist.",
        order = 130)
    public static void createConfigurationDirectory() throws IOException {
        File configurationDirectory = MCRConfigurationDir.getConfigurationDirectory();
        ArrayList<File> directories = new ArrayList<>(3);
        directories.add(configurationDirectory);
        for (String dir : MCRConfiguration.instance().getString("MCR.ConfigurationDirectory.template.directories", "")
            .split(",")) {
            if (!dir.trim().isEmpty()) {
                directories.add(new File(configurationDirectory, dir.trim()));
            }
        }
        for (File directory : directories) {
            if (!createDirectory(directory)) {
                break;
            }
        }

        for (String f : MCRConfiguration.instance().getString("MCR.ConfigurationDirectory.template.files", "")
            .split(",")) {
            if (!f.trim().isEmpty()) {
                createSampleConfigFile(f.trim());
            }
        }
    }

    private static boolean createDirectory(File directory) {
        if (directory.exists()) {
            LOGGER.warn("Directory {} already exists.", directory.getAbsolutePath());
            return true;
        }
        if (directory.mkdirs()) {
            LOGGER.info("Successfully created directory: {}", directory.getAbsolutePath());
            return true;
        } else {
            LOGGER.warn("Due to unknown error the directory could not be created: {}", directory.getAbsolutePath());
            return false;
        }
    }

    private static void createSampleConfigFile(String path) throws IOException {
        ClassLoader classLoader = MCRBasicCommands.class.getClassLoader();
        File configurationDirectory = MCRConfigurationDir.getConfigurationDirectory();
        File targetFile = new File(configurationDirectory, path);
        if (targetFile.exists()) {
            LOGGER.warn("File {} already exists.", targetFile.getAbsolutePath());
            return;
        }
        if (!targetFile.getParentFile().exists() && !targetFile.getParentFile().mkdirs()) {
            throw new IOException("Could not create directory for file: " + targetFile);
        }
        try (InputStream templateResource = classLoader.getResourceAsStream("configdir.template/" + path);
            FileOutputStream fout = new FileOutputStream(targetFile)) {
            if (templateResource == null) {
                throw new IOException("Could not find template for " + path);
            }
            IOUtils.copy(templateResource, fout);
            LOGGER.info("Created template for {} in {}", path, configurationDirectory);
        }
    }

    /**
     * The method parse and check an XML file.
     *
     * @param fileName
     *            the location of the xml file
     */
    @MCRCommand(syntax = "check file {0}", help = "Checks the data file {0} against the XML Schema.", order = 160)
    public static boolean checkXMLFile(String fileName) throws MCRException, SAXParseException, IOException {
        if (!fileName.endsWith(".xml")) {
            LOGGER.warn("{} ignored, does not end with *.xml", fileName);

            return false;
        }

        File file = new File(fileName);

        if (!file.isFile()) {
            LOGGER.warn("{} ignored, is not a file.", fileName);

            return false;
        }

        LOGGER.info("Reading file {} ...", file);
        MCRContent content = new MCRFileContent(file);

        MCRXMLParserFactory.getParser().parseXML(content);
        LOGGER.info("The file has no XML errors.");

        return true;
    }
}
