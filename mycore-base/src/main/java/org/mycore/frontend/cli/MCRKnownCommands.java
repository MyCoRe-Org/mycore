/*
 * $Revision$ 
 * $Date$
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

/**
 * Manages all known commands.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRKnownCommands {

    private static final String ANNOTATED = "Annotated";

    private final static Logger LOGGER = Logger.getLogger(MCRKnownCommands.class);

    protected static TreeMap<String, List<MCRCommand>> knownCommands = new TreeMap<String, List<MCRCommand>>();

    //    protected static List<MCRCommand> commands = new ArrayList<MCRCommand>();

    public MCRKnownCommands() {
        try {
            initCommands();
        } catch (Exception ex) {
            MCRCLIExceptionHandler.handleException(ex);
            System.exit(1);
        }
    }

    protected void initCommands() {
        initBuiltInCommands();
        initConfiguredCommands("Internal");
        initConfiguredCommands("External");
        initConfiguredCommands(ANNOTATED);
    }

    protected void initBuiltInCommands() {
        ArrayList<MCRCommand> commands = new ArrayList<MCRCommand>();
        commands.add(new MCRCommand("process {0}", "org.mycore.frontend.cli.MCRCommandLineInterface.readCommandsFile String",
            "Execute the commands listed in the text file {0}."));
        commands.add(new MCRCommand("help {0}", "org.mycore.frontend.cli.MCRKnownCommands.showCommandsHelp String",
            "Show the help text for the commands beginning with {0}."));
        commands.add(new MCRCommand("help", "org.mycore.frontend.cli.MCRKnownCommands.listKnownCommands", "List all possible commands."));
        commands.add(new MCRCommand("exit", "org.mycore.frontend.cli.MCRCommandLineInterface.exit", "Stop and exit the commandline tool."));
        commands.add(new MCRCommand("quit", "org.mycore.frontend.cli.MCRCommandLineInterface.exit", "Stop and exit the commandline tool."));
        commands.add(new MCRCommand("! {0}", "org.mycore.frontend.cli.MCRCommandLineInterface.executeShellCommand String",
            "Execute the shell command {0}, for example '! ls' or '! cmd /c dir'"));
        commands.add(new MCRCommand("show file {0}", "org.mycore.frontend.cli.MCRCommandLineInterface.show String", "Show contents of local file {0}"));
        commands.add(new MCRCommand("whoami", "org.mycore.frontend.cli.MCRCommandLineInterface.whoami", "Print the current user."));
        commands.add(new MCRCommand("show command statistics", "org.mycore.frontend.cli.MCRCommandStatistics.showCommandStatistics",
            "Show statistics on number of commands processed and execution time needed per command"));
        commands.add(new MCRCommand("cancel on error", "org.mycore.frontend.cli.MCRCommandLineInterface.cancelOnError",
            "Cancel execution of further commands in case of error"));
        commands.add(new MCRCommand("skip on error", "org.mycore.frontend.cli.MCRCommandLineInterface.skipOnError",
            "Skip execution of failed command in case of error"));
        commands.add(new MCRCommand("get uri {0} to file {1}", "org.mycore.frontend.cli.MCRCommandLineInterface.getURI String String",
            "Get XML content from URI {0} and save it to a local file {1}"));
        knownCommands.put("Basic commands", commands);
    }

    /** Read internal and/or external commands */
    private void initConfiguredCommands(String type) {
        String prefix = "MCR.CLI.Classes." + type;
        Properties p = MCRConfiguration.instance().getProperties(prefix);
        MCRCLIClassType cliClassType = null;

        if (ANNOTATED.equals(type)) {
            cliClassType = new MCRAnnotatedCLIClass();
        } else {
            cliClassType = new MCRDefaultCLIClass();
        }

        for (Object propertyName : p.keySet()) {
            String[] classNames = MCRConfiguration.instance().getString((String) propertyName, "").split(",");

            for (String className : classNames) {
                className = className.trim();
                if (className.isEmpty())
                    continue;

                LOGGER.debug("Will load commands from the " + type.toLowerCase() + " class " + className);
                cliClassType.add(className);
            }
        }
    }

    public interface MCRCLIClassType {
        public void add(String className);
    }

    private class MCRAnnotatedCLIClass implements MCRCLIClassType {
        @Override
        public void add(String className) {
            ArrayList<MCRCommand> commands = new ArrayList<MCRCommand>();
            try {
                Class<?> cliClass = Class.forName(className);
                String groupName;
                if (cliClass.isAnnotationPresent(MCRCommandGroup.class)) {
                    groupName = cliClass.getAnnotation(MCRCommandGroup.class).name();
                } else {
                    groupName = cliClass.getSimpleName();
                }
                Method[] methods = cliClass.getMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(org.mycore.frontend.cli.annotation.MCRCommand.class)) {
                        commands.add(new MCRCommand(method));
                    }
                }
                knownCommands.put(groupName, commands);
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    private class MCRDefaultCLIClass implements MCRCLIClassType {

        @Override
        public void add(String className) {
            Object obj = buildInstanceOfClass(className);
            MCRExternalCommandInterface commandInterface = (MCRExternalCommandInterface) obj;
            ArrayList<MCRCommand> commandsToAdd = commandInterface.getPossibleCommands();
            knownCommands.put(commandInterface.getDisplayName(), commandsToAdd);
        }

    }

    private Object buildInstanceOfClass(String className) {
        try {
            return Class.forName(className).newInstance();
        } catch (Exception ex) {
            String msg = "Could not instantiate class " + className;
            throw new MCRConfigurationException(msg, ex);
        }
    }

    public List<String> invokeCommand(String command) throws Exception {
        for (List<MCRCommand> commands : knownCommands.values()) {
            for (MCRCommand currentCommand : commands) {
                long start = System.currentTimeMillis();
                List<String> commandsReturned = currentCommand.invoke(command);
                long end = System.currentTimeMillis();

                if (commandsReturned != null) {
                    long timeNeeded = end - start;
                    MCRCommandLineInterface.output("Command processed (" + timeNeeded + " ms)");
                    MCRCommandStatistics.commandInvoked(currentCommand, timeNeeded);

                    return commandsReturned;
                }
            }
        }

        MCRCommandLineInterface.output("Command not understood:" + command);
        MCRCommandLineInterface.output("Enter 'help' to get a list of commands.");
        return new ArrayList<String>();
    }

    /**
     * Shows a list of commands understood by the command line interface and
     * shows their input syntax. This method implements the "help" command
     */
    public static void listKnownCommands() {
        MCRCommandLineInterface.output("The following commands can be used:");
        MCRCommandLineInterface.output("");

        for (List<MCRCommand> commands : knownCommands.values()) {
            for (MCRCommand command : commands) {
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
    public static void showCommandsHelp(String pattern) {
        boolean foundMatchingCommand = false;

        for (List<MCRCommand> commands : knownCommands.values()) {
            for (MCRCommand command : commands) {
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
}
