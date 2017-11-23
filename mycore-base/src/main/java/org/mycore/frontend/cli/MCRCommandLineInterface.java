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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.events.MCRStartupHandler;
import org.mycore.common.xml.MCRURIResolver;

/**
 * The main class implementing the MyCoRe command line interface. With the
 * command line interface, you can import, export, update and delete documents
 * and other data from/to the filesystem. Metadata is imported from and exported
 * to XML files. The command line interface is for administrative purposes and
 * to be used on the server side. It implements an interactive command prompt
 * and understands a set of commands. Each command is an instance of the class
 * <code>MCRCommand</code>.
 * 
 * @see MCRCommand
 * 
 * @author Frank Lützenkirchen
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 */
public class MCRCommandLineInterface {

    private static final Logger LOGGER = LogManager.getLogger();

    /** The name of the system */
    private static String system = null;

    /** A queue of commands waiting to be executed */
    protected static Vector<String> commandQueue = new Vector<>();

    protected static Vector<String> failedCommands = new Vector<>();

    private static boolean interactiveMode = true;

    private static boolean SKIP_FAILED_COMMAND = false;

    private static MCRCommandManager knownCommands;

    private static ThreadLocal<String> sessionId = new ThreadLocal<>();

    /**
     * The main method that either shows up an interactive command prompt or
     * reads a file containing a list of commands to be processed
     */
    public static void main(String[] args) {
        MCRStartupHandler.startUp(null/*no servlet context here*/);
        //BUG: try to track down https://bamboo.mycore.de/browse/DP-TEST-234
        if (MCRConfiguration.instance().getString("MCR.CommandLineInterface.SystemName", null) == null) {
            try {
                MCRConfiguration.instance().store(System.err,
                    "I'm going die soon, this should be my gravestone quote:");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //BUG DEBUG END
        system = MCRConfiguration.instance().getString("MCR.CommandLineInterface.SystemName") + ":";

        initSession();
        output("");
        output("Command Line Interface.");
        output("");

        output("Initializing...");
        knownCommands = new MCRCommandManager();
        output("Initialization done.");

        output("Type 'help' to list all commands!");
        output("");

        readCommandFromArguments(args);

        String command = null;

        MCRCommandPrompt prompt = new MCRCommandPrompt(system);
        while (true) {
            if (commandQueue.isEmpty()) {
                if (interactiveMode) {
                    command = prompt.readCommand();
                } else if (MCRConfiguration.instance().getString("MCR.CommandLineInterface.unitTest", "false")
                    .equals("true")) {
                    break;
                } else {
                    exit();
                }
            } else {
                command = commandQueue.firstElement();
                commandQueue.removeElementAt(0);
                System.out.println(system + "> " + command);
            }

            processCommand(command);
        }
    }

    private static void readCommandFromArguments(String[] args) {
        if (args.length > 0) {
            interactiveMode = false;

            String line = readLineFromArguments(args);
            addCommands(line);
        }
    }

    private static void addCommands(String line) {
        String[] cmds = line.split(";;");
        for (String cmd : cmds) {
            cmd = cmd.trim();
            if (!cmd.isEmpty()) {
                commandQueue.add(cmd);
            }
        }
    }

    private static String readLineFromArguments(String[] args) {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(arg).append(" ");
        }
        return sb.toString();
    }

    private static void initSession() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        session.setCurrentIP("127.0.0.1");
        session.setUserInformation(MCRSystemUserInformation.getSuperUserInstance());
        MCRSessionMgr.setCurrentSession(session);
        sessionId.set(session.getID());
        MCRSessionMgr.releaseCurrentSession();
    }

    /**
     * Processes a command entered by searching a matching command in the list
     * of known commands and executing its method.
     * 
     * @param command
     *            The command string to be processed
     */
    protected static void processCommand(String command) {

        MCRSession session = MCRSessionMgr.getSession(sessionId.get());
        MCRSessionMgr.setCurrentSession(session);

        try {
            session.beginTransaction();
            List<String> commandsReturned = knownCommands.invokeCommand(expandCommand(command));
            session.commitTransaction();
            addCommandsToQueue(commandsReturned);
        } catch (Exception ex) {
            MCRCLIExceptionHandler.handleException(ex);
            rollbackTransaction(session);
            if (SKIP_FAILED_COMMAND) {
                saveFailedCommand(command);
            } else {
                saveQueue(command);
                if (!interactiveMode) {
                    System.exit(1);
                }
                commandQueue.clear();
            }
        } finally {
            MCRSessionMgr.releaseCurrentSession();
        }
    }

    /**
     * Expands variables in a command.
     * Replaces any variables in form ${propertyName} to the value defined by {@link MCRConfiguration#getString(String)}.
     * If the property is not defined not variable replacement takes place.
     * @param command a CLI command that should be expanded
     * @return expanded command
     */
    public static String expandCommand(final String command) {
        StrSubstitutor strSubstitutor = new StrSubstitutor(MCRConfiguration.instance().getPropertiesMap());
        String expandedCommand = strSubstitutor.replace(command);
        if (!expandedCommand.equals(command)) {
            LOGGER.info("{} --> {}", command, expandedCommand);
        }
        return expandedCommand;
    }

    private static void rollbackTransaction(MCRSession session) {
        output("Command failed. Performing transaction rollback...");

        if (session.isTransactionActive()) {
            try {
                session.rollbackTransaction();
            } catch (Exception ex2) {
                MCRCLIExceptionHandler.handleException(ex2);
            }
        }
    }

    private static void addCommandsToQueue(List<String> commandsReturned) {
        if (commandsReturned.size() > 0) {
            output("Queueing " + commandsReturned.size() + " commands to process");

            for (int i = 0; i < commandsReturned.size(); i++) {
                commandQueue.insertElementAt(commandsReturned.get(i), i);
            }
        }
    }

    protected static void saveQueue(String lastCommand) {
        output("");
        output("The following command failed:");
        output(lastCommand);
        if (!commandQueue.isEmpty()) {
            System.out.printf(Locale.ROOT, "%s There are %s other commands still unprocessed.%n", system,
                commandQueue.size());
        } else if (interactiveMode) {
            return;
        }
        commandQueue.add(0, lastCommand);
        saveCommandQueueToFile(commandQueue, "unprocessed-commands.txt");
    }

    private static void saveCommandQueueToFile(final Vector<String> queue, String fname) {
        output("Writing unprocessed commands to file " + fname);
        try (PrintWriter pw = new PrintWriter(new File(fname), Charset.defaultCharset().name())) {
            for (String command : queue) {
                pw.println(command);
            }
            pw.close();
        } catch (IOException ex) {
            MCRCLIExceptionHandler.handleException(ex);
        }
    }

    protected static void saveFailedCommand(String lastCommand) {
        output("");
        output("The following command failed:");
        output(lastCommand);
        if (!commandQueue.isEmpty()) {
            System.out.printf(Locale.ROOT, "%s There are %s other commands still unprocessed.%n", system,
                commandQueue.size());
        }
        failedCommands.add(lastCommand);
    }

    protected static void handleFailedCommands() {
        if (failedCommands.size() > 0) {
            System.err.println(system + " Several command failed.");
            saveCommandQueueToFile(failedCommands, "failed-commands.txt");
        }
    }

    /**
     * Show contents of a local text file, including line numbers.
     * 
     * @param fname
     *            the filename
     */
    public static void show(String fname) throws Exception {
        AtomicInteger ln = new AtomicInteger();
        System.out.println();
        Files.readAllLines(Paths.get(fname), Charset.defaultCharset())
            .forEach(l -> System.out.printf(Locale.ROOT, "%04d: %s", ln.incrementAndGet(), l));
        System.out.println();
    }

    /**
     * Reads XML content from URIResolver and sends output to a local file.
     */
    public static void getURI(String uri, String file) throws Exception {
        Element resolved = MCRURIResolver.instance().resolve(uri);
        Element cloned = resolved.clone();
        new MCRJDOMContent(cloned).sendTo(new File(file));
    }

    /**
     * Reads a file containing a list of commands to be executed and adds them
     * to the commands queue for processing. This method implements the
     * "process ..." command.
     * 
     * @param file
     *            The file holding the commands to be processed
     * @throws IOException
     *             when the file could not be read
     * @throws FileNotFoundException
     *             when the file was not found
     */
    public static List<String> readCommandsFile(String file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(new File(file).toPath(), Charset.defaultCharset())) {
            output("Reading commands from file " + file);

            String line;
            List<String> list = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                list.add(line);
            }
            return list;
        }
    }

    /**
     * Executes simple shell commands from inside the command line interface and
     * shows their output. This method implements commands entered beginning
     * with exclamation mark, like "! ls -l /temp"
     * 
     * @param command
     *            the shell command to be executed
     * @throws IOException
     *             when an IO error occured while catching the output returned
     *             by the command
     * @throws SecurityException
     *             when the command could not be executed for security reasons
     */
    public static void executeShellCommand(String command) throws Exception {
        MCRExternalProcess process = new MCRExternalProcess(command);
        process.run();
        output(process.getOutput().asString());
        output(process.getErrors());
    }

    /**
     * Prints out the current user.
     */
    public static void whoami() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        String userName = session.getUserInformation().getUserID();
        output("You are user " + userName);
    }

    public static void cancelOnError() {
        SKIP_FAILED_COMMAND = false;
    }

    public static void skipOnError() {
        SKIP_FAILED_COMMAND = true;
    }

    /**
     * Exits the command line interface. This method implements the "exit" and
     * "quit" commands.
     */
    public static void exit() {
        showSessionDuration();
        handleFailedCommands();
        System.exit(0);
    }

    private static void showSessionDuration() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        long duration = System.currentTimeMillis() - session.getLoginTime();
        output("Session duration: " + duration + " ms");
    }

    static void output(String message) {
        System.out.println(system + " " + message);
    }

}
