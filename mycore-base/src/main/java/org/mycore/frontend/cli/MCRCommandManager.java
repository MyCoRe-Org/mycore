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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

/**
 * Manages all commands for the Command Line Interface and WebCLI.
 * 
 * @author Frank L\u00FCtzenkirchen
 * @author Robert Stephan
 */
public class MCRCommandManager {
    private final static Logger LOGGER = Logger.getLogger(MCRCommandManager.class);

    protected static TreeMap<String, List<MCRCommand>> knownCommands = new TreeMap<String, List<MCRCommand>>();

    public MCRCommandManager() {
        try {
            initCommands();
        } catch (Exception ex) {
            MCRCLIExceptionHandler.handleException(ex);
            System.exit(1);
        }
    }

    public static TreeMap<String, List<MCRCommand>> getKnownCommands(){
        return knownCommands;
    }
    protected void initCommands() {
        // load build-in commands
        addAnnotatedCLIClass(MCRBasicCommands.class);
        initConfiguredCommands("Internal");
        initConfiguredCommands("External");
    }

    /** Read internal and/or external commands */
    private void initConfiguredCommands(String type) {
        String prefix = "MCR.CLI.Classes." + type;
        Properties p = MCRConfiguration.instance().getProperties(prefix);
        for (Object propertyName : p.keySet()) {
            String[] classNames = MCRConfiguration.instance().getString((String) propertyName, "").split(",");

            for (String className : classNames) {
                className = className.trim();
                if (className.isEmpty()) {
                    continue;
                }
                LOGGER.debug("Will load commands from the " + type.toLowerCase() + " class " + className);
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
        ArrayList<MCRCommand> commands = new ArrayList<MCRCommand>();
        String groupName;
        if (cliClass.isAnnotationPresent(MCRCommandGroup.class)) {
            groupName = cliClass.getAnnotation(MCRCommandGroup.class).name();
        } else {
            groupName = cliClass.getSimpleName();
        }
        Method[] methods = cliClass.getMethods();
        final Class<org.mycore.frontend.cli.annotation.MCRCommand> mcrCommandAnnotation = org.mycore.frontend.cli.annotation.MCRCommand.class;
        Arrays.sort(methods, new Comparator<Method>() {
            @Override
            public int compare(Method m1, Method m2) {
                int im1 = -1, im2 = -1;
                if (m1.isAnnotationPresent(mcrCommandAnnotation)) {
                    im1 = m1.getAnnotation(mcrCommandAnnotation).order();
                }
                if (m2.isAnnotationPresent(mcrCommandAnnotation)) {
                    im2 = m2.getAnnotation(mcrCommandAnnotation).order();
                }
                return im2 - im1;
            }
        });
        for (Method method : methods) {
            if (method.isAnnotationPresent(mcrCommandAnnotation)) {
                commands.add(new MCRCommand(method));
            }
        }
        knownCommands.put(groupName, commands);
    }

    protected void addDefaultCLIClass(String className) {
        Object obj = buildInstanceOfClass(className);
        MCRExternalCommandInterface commandInterface = (MCRExternalCommandInterface) obj;
        ArrayList<MCRCommand> commandsToAdd = commandInterface.getPossibleCommands();
        knownCommands.put(commandInterface.getDisplayName(), commandsToAdd);
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
}
