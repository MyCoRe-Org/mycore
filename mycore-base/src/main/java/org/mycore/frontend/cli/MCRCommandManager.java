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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

/**
 * Manages all commands for the Command Line Interface and WebCLI.
 *
 * @author Frank L\u00FCtzenkirchen
 * @author Robert Stephan
 */
public class MCRCommandManager {
    private final static Logger LOGGER = LogManager.getLogger(MCRCommandManager.class);

    protected static TreeMap<String, List<MCRCommand>> knownCommands = new TreeMap<>();

    public MCRCommandManager() {
        try {
            initBuiltInCommands();
            initCommands();
        } catch (Exception ex) {
            handleInitException(ex);
        }
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("DM_EXIT")
    protected void handleInitException(Exception ex) {
        MCRCLIExceptionHandler.handleException(ex);
        System.exit(1);
    }

    public static TreeMap<String, List<MCRCommand>> getKnownCommands() {
        return knownCommands;
    }

    protected void initBuiltInCommands() {
        addAnnotatedCLIClass(MCRBasicCommands.class);
    }

    protected void initCommands() {
        // load build-in commands
        initConfiguredCommands("Internal");
        initConfiguredCommands("External");
    }

    /** Read internal and/or external commands */
    protected void initConfiguredCommands(String type) {
        String prefix = "MCR.CLI.Classes." + type;
        Map<String, String> p = MCRConfiguration.instance().getPropertiesMap(prefix);
        for (String propertyName : p.keySet()) {
            List<String> classNames = MCRConfiguration.instance()
                .getStrings(propertyName, MCRConfiguration.emptyList());

            for (String className : classNames) {
                className = className.trim();
                if (className.isEmpty()) {
                    continue;
                }
                LOGGER.debug("Will load commands from the " + type + " class " + className);
                try {
                    Class<?> cliClass = Class.forName(className);
                    if (cliClass.isAnnotationPresent(MCRCommandGroup.class)) {
                        addAnnotatedCLIClass(cliClass);
                    } else {
                        addDefaultCLIClass(className);
                    }

                } catch (ClassNotFoundException cnfe) {
                    LOGGER.error("MyCoRe Command Class " + className + " not found.");
                }
            }
        }
    }

    protected void addAnnotatedCLIClass(Class<?> cliClass) {
        String groupName = Optional.ofNullable(cliClass.getAnnotation(MCRCommandGroup.class))
            .map(MCRCommandGroup::name)
            .orElse(cliClass.getSimpleName());
        final Class<org.mycore.frontend.cli.annotation.MCRCommand> mcrCommandAnnotation = org.mycore.frontend.cli.annotation.MCRCommand.class;
        ArrayList<MCRCommand> commands = Arrays.stream(cliClass.getMethods())
            .filter(method -> method.getDeclaringClass().equals(cliClass))
            .filter(method -> Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers()))
            .filter(method -> method.isAnnotationPresent(mcrCommandAnnotation))
            .sorted(Comparator.comparingInt(m -> m.getAnnotation(mcrCommandAnnotation).order()))
            .map(MCRCommand::new)
            .collect(Collectors.toCollection(ArrayList::new));
        addCommandGroup(groupName, commands);
    }

    //fixes MCR-1594 deep in the code
    private List<MCRCommand> addCommandGroup(String groupName, ArrayList<MCRCommand> commands) {
        return knownCommands.put(groupName, Collections.unmodifiableList(commands));
    }

    protected void addDefaultCLIClass(String className) {
        Object obj = buildInstanceOfClass(className);
        MCRExternalCommandInterface commandInterface = (MCRExternalCommandInterface) obj;
        ArrayList<MCRCommand> commandsToAdd = commandInterface.getPossibleCommands();
        addCommandGroup(commandInterface.getDisplayName(), commandsToAdd);
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
        return new ArrayList<>();
    }
}
