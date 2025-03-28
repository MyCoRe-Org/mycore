/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRClassTools;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

/**
 * Manages all commands for the Command Line Interface and WebCLI.
 *
 * @author Frank Lützenkirchen
 * @author Robert Stephan
 */
@SuppressWarnings("PMD.DoNotTerminateVM")
public class MCRCommandManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final SortedMap<String, List<MCRCommand>> KNOWN_COMMANDS = new TreeMap<>();

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

    public static SortedMap<String, List<MCRCommand>> getKnownCommands() {
        return KNOWN_COMMANDS;
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
        Stream<Map.Entry<String, String>> propsWithPrefix = MCRConfiguration2.getPropertiesMap()
            .entrySet()
            .stream()
            .filter(e -> e.getKey().startsWith(prefix));
        Stream<String> classNames = propsWithPrefix
            .map(Map.Entry::getValue)
            .flatMap(MCRConfiguration2::splitValue)
            .filter(s -> !s.isEmpty());
        classNames.forEach(this::loadCommandClass);
    }

    private void loadCommandClass(String commandClassName) {
        LOGGER.debug("Will load commands from the {} class {}", commandClassName, commandClassName);
        try {
            Class<?> cliClass = MCRClassTools.forName(commandClassName);
            if (cliClass.isAnnotationPresent(MCRCommandGroup.class)) {
                addAnnotatedCLIClass(cliClass);
            } else {
                addDefaultCLIClass(commandClassName);
            }

        } catch (ClassNotFoundException cnfe) {
            LOGGER.error("MyCoRe Command Class {} not found.", commandClassName);
        }
    }

    protected void addAnnotatedCLIClass(Class<?> cliClass) {
        String groupName = Optional.ofNullable(cliClass.getAnnotation(MCRCommandGroup.class))
            .map(MCRCommandGroup::name)
            .orElse(cliClass.getSimpleName());
        final Class<org.mycore.frontend.cli.annotation.MCRCommand> mcrCommandAnnotation;
        mcrCommandAnnotation = org.mycore.frontend.cli.annotation.MCRCommand.class;
        List<MCRCommand> commands = Arrays.stream(cliClass.getMethods())
            .filter(method -> method.getDeclaringClass().equals(cliClass))
            .filter(method -> Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers()))
            .filter(method -> method.isAnnotationPresent(mcrCommandAnnotation))
            .sorted(Comparator.comparingInt(m -> m.getAnnotation(mcrCommandAnnotation).order()))
            .map(MCRCommand::new)
            .collect(Collectors.toCollection(ArrayList::new));
        addCommandGroup(groupName, commands);
    }

    //fixes MCR-1594 deep in the code
    private List<MCRCommand> addCommandGroup(String groupName, List<MCRCommand> commands) {
        return KNOWN_COMMANDS.put(groupName, Collections.unmodifiableList(commands));
    }

    protected void addDefaultCLIClass(String className) {
        Object obj = buildInstanceOfClass(className);
        MCRExternalCommandInterface commandInterface = (MCRExternalCommandInterface) obj;
        List<MCRCommand> commandsToAdd = commandInterface.getPossibleCommands();
        addCommandGroup(commandInterface.getDisplayName(), commandsToAdd);
    }

    private Object buildInstanceOfClass(String className) {
        try {
            return MCRClassTools.forName(className).getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            String msg = "Could not instantiate class " + className;
            throw new MCRConfigurationException(msg, ex);
        }
    }

    public List<String> invokeCommand(String command) throws Exception {
        if (command.trim().startsWith("#")) {
            //ignore comment
            return new ArrayList<>();
        }
        for (List<MCRCommand> commands : KNOWN_COMMANDS.values()) {
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
