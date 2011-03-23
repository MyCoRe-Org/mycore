/*
 * 
 * $Revision$ $Date$
 * 
 * This file is part of M y C o R e See http://www.mycore.de/ for details.
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.hibernate.Transaction;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.ifs2.MCRContent;

/**
 * The main class implementing the MyCoRe command line interface. With the command line interface, you can import, export, update and delete documents and other
 * data from/to the filesystem. Metadata is imported from and exported to XML files. The command line interface is for administrative purposes and to be used on
 * the server side. It implements an interactive command prompt and understands a set of commands. Each command is an instance of the class
 * <code>MCRCommand</code>.
 * 
 * @see MCRCommand
 * @author Frank Lützenkirchen
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRCommandLineInterface {
    /** The Logger */
    static Logger logger = Logger.getLogger(MCRCommandLineInterface.class);

    /** The name of the system */
    private static String system = null;

    /** The array holding all known commands */
    protected static ArrayList<MCRCommand> knownCommands = new ArrayList<MCRCommand>();

    /** A queue of commands waiting to be executed */
    protected static Vector<String> commandQueue = new Vector<String>();

    protected static Vector<String> failedCommands = new Vector<String>();

    /** The standard input console where the user enters commands */
    protected static BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

    protected static ConcurrentLinkedQueue<Number> benchList = new ConcurrentLinkedQueue<Number>();

    private static boolean interactiveMode = true;

    private static boolean SKIP_FAILED_COMMAND = false;

    private static void initKnownCommands() {
        initBuiltInCommands();
        initConfiguredCommands("Internal");
        initConfiguredCommands("External");
    }
    
    private static void initBuiltInCommands() {
        knownCommands.add(new MCRCommand("process {0}", "org.mycore.frontend.cli.MCRCommandLineInterface.readCommandsFile String", "Execute the commands listed in the text file {0}."));
        knownCommands.add(new MCRCommand("help {0}", "org.mycore.frontend.cli.MCRCommandLineInterface.showCommandsHelp String", "Show the help text for the commands beginning with {0}."));
        knownCommands.add(new MCRCommand("help", "org.mycore.frontend.cli.MCRCommandLineInterface.listKnownCommands", "List all possible commands."));
        knownCommands.add(new MCRCommand("exit", "org.mycore.frontend.cli.MCRCommandLineInterface.exit", "Stop and exit the commandline tool."));
        knownCommands.add(new MCRCommand("quit", "org.mycore.frontend.cli.MCRCommandLineInterface.exit", "Stop and exit the commandline tool."));
        knownCommands.add(new MCRCommand("! {0}", "org.mycore.frontend.cli.MCRCommandLineInterface.executeShellCommand String", "Execute the shell command {0}, for example '! ls' or '! cmd /c dir'"));
        knownCommands.add(new MCRCommand("show file {0}", "org.mycore.frontend.cli.MCRCommandLineInterface.show String", "Show contents of local file {0}"));
        knownCommands.add(new MCRCommand("whoami", "org.mycore.frontend.cli.MCRCommandLineInterface.whoami", "Print the current user."));
        knownCommands.add(new MCRCommand("show command statistics", "org.mycore.frontend.cli.MCRCommandLineInterface.showCommandStatistics", "Show statistics on number of commands processed and execution time needed per command"));
        knownCommands.add(new MCRCommand("cancel on error", "org.mycore.frontend.cli.MCRCommandLineInterface.cancelOnError", "Cancel execution of further commands in case of error"));
        knownCommands.add(new MCRCommand("skip on error", "org.mycore.frontend.cli.MCRCommandLineInterface.skipOnError", "Skip execution of failed command in case of error"));
        knownCommands.add(new MCRCommand("get uri {0} to file {1}", "org.mycore.frontend.cli.MCRCommandLineInterface.getURI String String", "Get XML content from URI {0} and save it to a local file {1}"));
    }

    /** Read internal and/or external commands */
    private static void initConfiguredCommands(String type) {
        String propertyName = "MCR.CLI.Classes." + type;
        String[] classNames = MCRConfiguration.instance().getString(propertyName, "").split(",");

        for (String className : classNames) {
            logger.debug("Will load commands from the " + type.toLowerCase() + " class " + className);
            addKnownCommandsFromClass(className);
        }
    }

    private static void addKnownCommandsFromClass(String className) {
        Object obj = buildInstanceOfClass(className);
        ArrayList<MCRCommand> commands = ((MCRExternalCommandInterface) obj).getPossibleCommands();
        knownCommands.addAll(commands);
    }

    private static Object buildInstanceOfClass(String className) {
        try {
            return Class.forName(className).newInstance();
        } catch (Exception ex) {
            String msg = "Could not instantiate class " + className;
            throw new MCRConfigurationException(msg, ex);
        }
    }

    /**
     * The main method that either shows up an interactive command prompt or reads a file containing a list of commands to be processed
     */
    public static void main(String[] args) {
        system = MCRConfiguration.instance().getString("MCR.CommandLineInterface.SystemName", "MyCoRe") + ": foo ";
        initSession();

        System.out.println();
        output("Command Line Interface.");
        output("");
        output("Initializing...");

        try {
            initKnownCommands();
        } catch (Exception ex) {
            showException(ex);
            System.exit(1);
        }

        output("Initialization done.");
        output("Type 'help' to list all commands!");
        output("");

        readCommandFromArguments(args);

        String command = null;
        String firstCommand = null;

        while ( ! shouldExit ) {
            if (commandQueue.isEmpty()) {
                if (interactiveMode) {
                    command = readCommandFromPrompt();
                } else {
                    if (firstCommand != null && shouldSaveRuntimeStatistics()) {
                        try {
                            saveMillis(firstCommand);
                        } catch (IOException ex) {
                            showException(ex);
                        }
                    }
                    exit();
                    break;
                }
            } else {
                command = (String) commandQueue.firstElement();
                if (firstCommand == null) {
                    firstCommand = command;
                }
                commandQueue.removeElementAt(0);
                System.out.println(system + "> " + command);
            }

            processCommand(command);
        }
    }
    
    private static boolean shouldSaveRuntimeStatistics() {
        String property = "MCR.CLI.SaveRuntimeStatistics";
        return MCRConfiguration.instance().getBoolean(property, false);
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
        StringBuffer sb = new StringBuffer();
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
    }

    /**
     * Shows up a command prompt.
     * 
     * @return The command entered by the user at stdin
     */
    protected static String readCommandFromPrompt() {
        String line = "";
        do {
            line = readLineFromConsole();
        } while (line.isEmpty());
        return line;
    }

    private static String readLineFromConsole() {
        System.out.print(system + "> ");

        try {
            return console.readLine().trim();
        } catch (IOException ignored) {
            return "";
        }
    }

    /** Stores total time needed for all executions of the given command */
    protected static HashMap<String, Long> timeNeeded = new HashMap<String, Long>();

    /** Stores total number of executions for each command */
    protected static HashMap<String, Integer> numInvocations = new HashMap<String, Integer>();

    /**
     * Processes a command entered by searching a matching command in the list of known commands and executing its method.
     * 
     * @param command
     *            The command string to be processed
     */
    protected static void processCommand(String command) {
        long start = 0, end = 0;
        Transaction tx = null;
        List<String> commandsReturned = null;
        String invokedCommand = null;

        try {
            tx = MCRHIBConnection.instance().getSession().beginTransaction();
            for (MCRCommand currentCommand : knownCommands) {
                start = System.currentTimeMillis();
                commandsReturned = currentCommand.invoke(command);

                if (commandsReturned != null) // Command was executed
                {
                    end = System.currentTimeMillis();
                    invokedCommand = currentCommand.showSyntax();

                    long sum = timeNeeded.containsKey(invokedCommand) ? timeNeeded.get(invokedCommand) : 0L;
                    sum += end - start;
                    timeNeeded.put(invokedCommand, sum);

                    int num = 1 + (numInvocations.containsKey(invokedCommand) ? numInvocations.get(invokedCommand) : 0);
                    numInvocations.put(invokedCommand, num);

                    // Add commands to queue
                    if (commandsReturned.size() > 0) {
                        System.out.println(system + " Queueing " + commandsReturned.size() + " commands to process");

                        for (int i = 0; i < commandsReturned.size(); i++) {
                            commandQueue.insertElementAt(commandsReturned.get(i), i);
                        }
                    }

                    break;
                }
            }
            tx.commit();
            if (commandsReturned != null) {
                System.out.printf("%s Command processed (%d ms)\n", system, (end - start));
                addMillis(end - start);
            } else {
                if (interactiveMode) {
                    System.out.printf("%s Command not understood. Enter 'help' to get a list of commands.\n", system);
                } else {
                    throw new MCRUsageException("Command not understood: " + command);
                }
            }
        } catch (Exception ex) {
            showException(ex);
            System.out.printf("%s Command failed. Performing transaction rollback...\n", system);
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Exception ex2) {
                    showException(ex2);
                }
            }
            if (SKIP_FAILED_COMMAND) {
                saveFailedCommand(command);
            } else {
                saveQueue(command);
                if (!interactiveMode) {
                    System.exit(1);
                }
            }
        } finally {
            if (tx != null) {
                tx = MCRHIBConnection.instance().getSession().beginTransaction();
                MCRHIBConnection.instance().getSession().clear();
                tx.commit();
            }
        }
    }

    /**
     * Shows statistics on number of invocations and time needed for each command successfully executed.
     */
    public static void showCommandStatistics() {
        System.out.println();
        for (Object key : timeNeeded.keySet().toArray()) {
            long tn = timeNeeded.get(key);
            int num = numInvocations.get(key);

            System.out.println(key);
            System.out.println("  total: " + tn + " ms, average: " + tn / num + " ms, " + num + " invocations.");
        }
        System.out.println();
    }

    protected static void saveQueue(String lastCommand) {
        output("");
        output("The following command failed:");
        output(lastCommand);
        if (!commandQueue.isEmpty()) {
            System.out.printf("%s There are %s other commands still unprocessed.\n", system, commandQueue.size());
        } else if (interactiveMode) {
            return;
        }
        commandQueue.add(0, lastCommand);
        saveCommandQueueToFile(commandQueue, "unprocessed-commands.txt");
    }

    private static void saveCommandQueueToFile(final Vector<String> queue, String fname) {
        output("Writing unprocessed commands to file " + fname);

        try {
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fname)));
            for (String command : queue) {
                pw.println(command);
            }
            pw.close();
        } catch (IOException ex) {
            showException(ex);
        }
    }

    protected static void saveFailedCommand(String lastCommand) {
        output("");
        output("The following command failed:");
        output(lastCommand);
        if (!commandQueue.isEmpty()) {
            System.out.printf("%s There are %s other commands still unprocessed.\n", system, commandQueue.size());
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
     * @throws Exception
     */
    public static void show(String fname) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(fname));
        System.out.println();
        String line;
        int i = 1;
        while ((line = br.readLine()) != null) {
            System.out.printf("%04d: %s\n", i++, line);
        }
        br.close();
        System.out.println();
    }

    /**
     * Reads XML content from URIResolver and sends output to a local file.
     */
    public static void getURI(String uri, String file) throws Exception {
        Element resolved = MCRURIResolver.instance().resolve(uri);
        Element cloned = (Element) resolved.clone();
        MCRContent.readFrom(new Document(cloned)).sendTo(new File(file));
    }

    /**
     * Shows details about an exception that occured during command processing
     * 
     * @param ex
     *            The exception that was caught while processing a command
     */
    protected static void showException(Throwable ex) {
        if (ex instanceof InvocationTargetException) {
            ex = ((InvocationTargetException) ex).getTargetException();
            showException(ex);
            return;
        } else if (ex instanceof ExceptionInInitializerError) {
            ex = ((ExceptionInInitializerError) ex).getCause();
            showException(ex);
            return;
        }

        output("");
        output("Exception occured: " + ex.getClass().getName());
        output("Exception message: " + ex.getLocalizedMessage());
        output("");

        String trace = MCRException.getStackTraceAsString(ex);
        if (logger.isDebugEnabled()) {
            logger.debug(trace);
        } else {
            output(trace);
        }

        if (ex instanceof MCRActiveLinkException) {
            showActiveLinks((MCRActiveLinkException)ex);
        }
        if (ex instanceof MCRException) {
            ex = ((MCRException) ex).getCause();
            if (ex != null) {
                output("");
                output("This exception was caused by:");
                showException(ex);
            }
        }
    }

    private static void showActiveLinks(MCRActiveLinkException activeLinks) {
        output("There are links active preventing the commit of work, see error message for details.");
        output("The following links where affected:");

        Map<String, Collection<String>> links = activeLinks.getActiveLinks();

        for (String curDest : links.keySet()) {
            logger.debug("Current Destination: " + curDest);
            Collection<String> sources = links.get(curDest);
            for (String source : sources)
                output(source + " ==> " + curDest);
        }
    }

    /**
     * Reads a file containing a list of commands to be executed and adds them to the commands queue for processing. This method implements the "process ..."
     * command.
     * 
     * @param file
     *            The file holding the commands to be processed
     * @throws IOException
     *             when the file could not be read
     * @throws FileNotFoundException
     *             when the file was not found
     */
    public static List<String> readCommandsFile(String file) throws IOException, FileNotFoundException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        output("Reading commands from file " + file);

        String line;
        List<String> list = new ArrayList<String>();
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }
            list.add(line);
        }

        reader.close();
        return list;
    }

    /**
     * Shows a list of commands understood by the command line interface and shows their input syntax. This method implements the "help" command
     */
    public static void listKnownCommands() {
        output("The following " + knownCommands.size() + " commands can be used:");
        output("");

        for (MCRCommand command : knownCommands) {
            output(command.showSyntax());
        }
    }

    /**
     * Shows the help text for one or more commands.
     * 
     * @param com the command, or a fragment of it
     */
    public static void showCommandsHelp(String com) {
        boolean foundMatchingCommand = false;

        for (MCRCommand command : knownCommands) {
            if (command.showSyntax().contains(com)) {
                showCommandHelp(command);
                foundMatchingCommand = true;
            }
        }

        if (!foundMatchingCommand) {
            output("Unknown command:" + com);
        }
    }

    private static void showCommandHelp(MCRCommand command) {
        output(command.showSyntax());
        output("    " + command.getHelpText());
        output("");
    }

    /**
     * Executes simple shell commands from inside the command line interface and shows their output. 
     * This method implements commands entered beginning with exclamation mark, like 
     * "! ls -l /temp"
     * 
     * @param command
     *            the shell command to be executed
     * @throws IOException
     *             when an IO error occured while catching the output returned by the command
     * @throws SecurityException
     *             when the command could not be executed for security reasons
     */
    public static void executeShellCommand(String command) throws IOException, SecurityException {
        Process p = Runtime.getRuntime().exec(command);
        showOutput(p.getInputStream());
        showOutput(p.getErrorStream());
    }

    /**
     * Prints out the current user.
     */
    public static void whoami() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        String userName = session.getUserInformation().getCurrentUserID();
        output("You are user " + userName);
    }

    /**
     * Catches the output read from an input stream and prints it line by line on standard out. 
     * This is used to catch the stdout and stderr stream output when
     * executing an external shell command.
     */
    private static void showOutput(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MCRUtils.copyStream(in, out);
        out.close();
        output(out.toString());
    }

    public static void cancelOnError() {
        SKIP_FAILED_COMMAND = false;
    }

    public static void skipOnError() {
        SKIP_FAILED_COMMAND = true;
    }

    public static void addMillis(long l) {
        benchList.add(l);
    }

    public static void clearMillis() {
        benchList.clear();
    }

    public static void saveMillis(String fileBaseName) throws IOException {
        PrintStream fout = new PrintStream(fileBaseName + ".dat");

        for (int i = 1; !benchList.isEmpty(); i++) {
            fout.printf("%d %d\n", i, benchList.poll().intValue());
        }
        fout.close();
    }

    private static boolean shouldExit = false;
    
    /**
     * Exits the command line interface. This method implements the "exit" and "quit" commands.
     */
    public static void exit() {
        showSessionDuration();
        handleFailedCommands();
        shouldExit = true;
    }

    private static void showSessionDuration() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        long duration = System.currentTimeMillis() - session.getLoginTime();
        output("Session duration: " + duration + " ms");
    }
    
    private static void output(String message) {
        System.out.println(system + " " + message);
    }
}
