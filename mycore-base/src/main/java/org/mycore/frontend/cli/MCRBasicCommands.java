/*
 * $Revision: 24802 $ 
 * $Date: 2012-08-01 15:23:08 +0200 (Mi, 01 Aug 2012) $
 * 
 * This file is part of   M y C o R e 
 * See http://www.mycore.de/ for details.
 * 
 * This program is free software; you can use it, redistribute it and / or modify it under the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation; either version 2 of the License or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program, in a file called gpl.txt or license.txt. If not, write to the Free
 * Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307 USA
 */

package org.mycore.frontend.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.common.MCRException;
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
    private static Logger LOGGER = Logger.getLogger(MCRBasicCommands.class);

    /**
     * Shows a list of commands understood by the command line interface and
     * shows their input syntax. This method implements the "help" command
     */
    @MCRCommand(syntax = "help", help = "List all possible commands.", order = 20)
    public static void listKnownCommands() {
        MCRCommandLineInterface.output("The following commands can be used:");
        MCRCommandLineInterface.output("");

        for (List<org.mycore.frontend.cli.MCRCommand> commands : MCRCommandManager.getKnownCommands().values()) {
            for (org.mycore.frontend.cli.MCRCommand command : commands) {
                MCRCommandLineInterface.output(command.getSyntax());
            }
        }
    }

    /**
     * Shows the help text for one or more commands.
     * 
     * @param pattern
     *            the command, or a fragment of it
     */

    @MCRCommand(syntax = "help {0}", help = "Show the help text for the commands beginning with {0}.", order = 10)
    public static void listKnownCommandsBeginningWithPrefix(String pattern) {
        boolean foundMatchingCommand = false;

        for (List<org.mycore.frontend.cli.MCRCommand> commands : MCRCommandManager.getKnownCommands().values()) {
            for (org.mycore.frontend.cli.MCRCommand command : commands) {
                if (command.getSyntax().contains(pattern)) {
                    command.outputHelp();
                    foundMatchingCommand = true;
                }
            }
        }

        if (!foundMatchingCommand) {
            MCRCommandLineInterface.output("Unknown command:" + pattern);
        }
    }

    @MCRCommand(syntax = "process {0}", help = "Execute the commands listed in the text file {0}.", order = 30)
    public static List<String> readCommandsFile(String file) throws IOException, FileNotFoundException {
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

    @MCRCommand(syntax = "! {0}", help = "Execute the shell command {0}, for example '! ls' or '! cmd /c dir'", order = 60)
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

    @MCRCommand(syntax = "show command statistics", help = "Show statistics on number of commands processed and execution time needed per command", order = 90)
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

    @MCRCommand(syntax = "get uri {0} to file {1}", help = "Get XML content from URI {0} and save it to a local file {1}", order = 120)
    public static void getURI(String uri, String file) throws Exception {
        MCRCommandLineInterface.getURI(uri, file);
    }

    @MCRCommand(syntax = "create configuration directory", help = "Creates the MCRConfiguration directory if it does not exist.", order = 130)
    public static void createConfigurationDirectory() {
        File configurationDirectory = MCRConfigurationDir.getConfigurationDirectory();
        ArrayList<File> directories = new ArrayList<>(3);
        directories.add(configurationDirectory);
        directories.add(new File(configurationDirectory, "lib"));
        directories.add(new File(configurationDirectory, "resources"));
        for (File directory : directories) {
            if (!createDirectory(directory)) {
                break;
            }
        }
    }

    private static boolean createDirectory(File directory) {
        Logger logger = Logger.getLogger(MCRBasicCommands.class);
        if (directory.exists()) {
            logger.warn("Directory " + directory.getAbsolutePath() + " already exists.");
            return true;
        }
        if (directory.mkdirs()) {
            logger.info("Successfully created directory: " + directory.getAbsolutePath());
            return true;
        } else {
            logger.warn("Due to unknown error the directory could not be created: " + directory.getAbsolutePath());
            return false;
        }
    }

    /**
     * The method parse and check an XML file.
     * 
     * @param fileName
     *            the location of the xml file
     * @throws SAXParseException 
     * @throws MCRException 
     */
    @MCRCommand(syntax = "check file {0}", help = "Checks the data file {0} against the XML Schema.", order = 160)
    public static boolean checkXMLFile(String fileName) throws MCRException, SAXParseException, IOException {
        if (!fileName.endsWith(".xml")) {
            LOGGER.warn(fileName + " ignored, does not end with *.xml");

            return false;
        }

        File file = new File(fileName);

        if (!file.isFile()) {
            LOGGER.warn(fileName + " ignored, is not a file.");

            return false;
        }

        LOGGER.info("Reading file " + file + " ...");
        MCRContent content = new MCRFileContent(file);

        MCRXMLParserFactory.getParser().parseXML(content);
        LOGGER.info("The file has no XML errors.");

        return true;
    }
}
