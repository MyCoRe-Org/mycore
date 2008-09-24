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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.hibernate.Transaction;

import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.user.MCRUserMgr;

/**
 * The main class implementing the MyCoRe command line interface. With the command line interface, you can import, export, update and delete documents and other
 * data from/to the filesystem. Metadata is imported from and exported to XML files. The command line interface is for administrative purposes and to be used on
 * the server side. It implements an interactive command prompt and understands a set of commands. Each command is an instance of the class
 * <code>MCRCommand</code>.
 * 
 * @see MCRCommand
 * @author Frank L\u00fctzenkirchen
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRCommandLineInterface {
    /** The Logger */
    static Logger logger;

    /** The name of the system */
    private static String system = null;

    /** The configuration */
    private static MCRConfiguration config = null;

    /** The array holding all known commands */
    protected static ArrayList<MCRCommand> knownCommands = new ArrayList<MCRCommand>();

    /** A queue of commands waiting to be executed */
    protected static Vector<String> commandQueue = new Vector<String>();

    protected static Vector<String> failedCommands = new Vector<String>();

    /** The standard input console where the user enters commands */
    protected static BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

    /** The current session */
    private static MCRSession session = null;

    private static boolean interactiveMode = true;

    private static boolean SKIP_FAILED_COMMAND = false;

    /**
     * Reads command definitions from a configuration file and builds the MCRCommand instances
     */
    protected static void initCommands() {
        // **************************************
        // Built-in commands
        // **************************************
        knownCommands.add(new MCRCommand("process {0}", "org.mycore.frontend.cli.MCRCommandLineInterface.readCommandsFile String",
                "Execute the commands listed in the text file {0}."));
        knownCommands.add(new MCRCommand("help {0}", "org.mycore.frontend.cli.MCRCommandLineInterface.showCommandsHelp String",
                "Show the help text for the commands beginning with {0}."));
        knownCommands.add(new MCRCommand("help", "org.mycore.frontend.cli.MCRCommandLineInterface.listKnownCommands", "List all possible commands."));
        knownCommands.add(new MCRCommand("exit", "org.mycore.frontend.cli.MCRCommandLineInterface.exit", "Stop and exit the commandline tool."));
        knownCommands.add(new MCRCommand("quit", "org.mycore.frontend.cli.MCRCommandLineInterface.exit", "Stop and exit the commandline tool."));
        knownCommands.add(new MCRCommand("! {0}", "org.mycore.frontend.cli.MCRCommandLineInterface.executeShellCommand String",
                "Execute the shell command {0}, for example '! ls' or '! cmd /c dir'"));
        knownCommands.add(new MCRCommand("show file {0}", "org.mycore.frontend.cli.MCRCommandLineInterface.show String", "Show contents of local file {0}"));
        knownCommands.add(new MCRCommand("change to user {0} with {1}", "org.mycore.frontend.cli.MCRCommandLineInterface.changeToUser String String",
                "Change the user {0} with the given password in {1}."));
        knownCommands.add(new MCRCommand("login {0}", "org.mycore.frontend.cli.MCRCommandLineInterface.login String",
                "Start the login dialog for the user {0}."));
        knownCommands.add(new MCRCommand("whoami", "org.mycore.frontend.cli.MCRCommandLineInterface.whoami", "Print the current user."));
        knownCommands.add(new MCRCommand("show command statistics", "org.mycore.frontend.cli.MCRCommandLineInterface.showCommandStatistics",
                "Show statistics on number of commands processed and execution time needed per command"));
        knownCommands.add(new MCRCommand("cancel on error", "org.mycore.frontend.cli.MCRCommandLineInterface.cancelOnError",
                "Cancel execution of further commands in case of error"));
        knownCommands.add(new MCRCommand("skip on error", "org.mycore.frontend.cli.MCRCommandLineInterface.skipOnError",
                "Skip execution of failed command in case of error"));

        // **************************************
        // Read internal and/or external commands
        // **************************************
        readCommands("MCR.CLI.Classes.Internal", "internal");
        readCommands("MCR.CLI.Classes.External", "external");
    }

    private static void readCommands(String property, String type) {
        String classes = config.getString(property, "");

        for (StringTokenizer st = new StringTokenizer(classes, ","); st.hasMoreTokens();) {
            String classname = st.nextToken();
            logger.debug("Will load commands from the " + type + " class " + classname);

            Object obj;
            try {
                obj = Class.forName(classname).newInstance();
            } catch (Exception e) {
                String msg = "Could not instantiate class " + classname;
                throw new org.mycore.common.MCRConfigurationException(msg, e);
            }
            ArrayList<MCRCommand> commands = ((MCRExternalCommandInterface) obj).getPossibleCommands();
            knownCommands.addAll(commands);
        }
    }

    /**
     * The main method that either shows up an interactive command prompt or reads a file containing a list of commands to be processed
     */
    public static void main(String[] args) {
        config = MCRConfiguration.instance();
        logger = Logger.getLogger(MCRCommandLineInterface.class);
        session = MCRSessionMgr.getCurrentSession();
        session.setCurrentIP(MCRSession.getLocalIP());
        MCRSessionMgr.setCurrentSession(session);
        system = config.getString("MCR.CommandLineInterface.SystemName", "MyCoRe") + ":";

        System.out.println();
        System.out.println(system + " Command Line Interface.");
        System.out.println(system);
        System.out.println(system + " Initializing...");

        try {
            initCommands();
        } catch (Exception ex) {
            showException(ex);
            System.exit(1);
        }

        System.out.println(system + " Initialization done.");
        System.out.println(system + " Type 'help' to list all commands!");
        System.out.println(system);

        if (args.length > 0) {
            interactiveMode = false;

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < args.length; i++)
                sb.append(args[i]).append(" ");
            String line = sb.toString().trim();
            String[] cmds = line.split(";;");
            for (String cmd : cmds) {
                cmd = cmd.trim();
                if (cmd.length() > 0)
                    commandQueue.add(cmd);
            }
        }

        String command = null;

        while (true) {
            if (commandQueue.isEmpty()) {
                if (interactiveMode) {
                    command = readCommandFromPrompt();
                } else {
                    exit();
                    // break;
                }
            } else {
                command = (String) commandQueue.firstElement();
                commandQueue.removeElementAt(0);
                System.out.println(system + "> " + command);
            }

            processCommand(command);
        }
    }

    /**
     * Shows up a command prompt.
     * 
     * @return The command entered by the user at stdin
     */
    protected static String readCommandFromPrompt() {
        String line = "";

        do {
            System.out.print(system + "> ");

            try {
                line = console.readLine();
            } catch (IOException ex) {
            }
        } while ((line = line.trim()).length() == 0);

        return line;
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
        Transaction tx = MCRHIBConnection.instance().getSession().beginTransaction();
        List<String> commandsReturned = null;
        String invokedCommand = null;

        try {
            for (MCRCommand currentCommand : knownCommands) {
                start = System.currentTimeMillis();
                commandsReturned = currentCommand.invoke(command);

                if (commandsReturned != null) // Command was executed
                {
                    end = System.currentTimeMillis();
                    invokedCommand = currentCommand.showSyntax();

                    long sum = (timeNeeded.containsKey(invokedCommand) ? timeNeeded.get(invokedCommand) : 0L);
                    sum += end - start;
                    timeNeeded.put(invokedCommand, sum);

                    int num = 1 + (numInvocations.containsKey(invokedCommand) ? numInvocations.get(invokedCommand) : 0);
                    numInvocations.put(invokedCommand, num);

                    // Add commands to queue
                    if (commandsReturned.size() > 0) {
                        System.out.println(system + " Queueing " + commandsReturned.size() + " commands to process");

                        for (int i = 0; i < commandsReturned.size(); i++)
                            commandQueue.insertElementAt(commandsReturned.get(i), i);
                    }

                    break;
                }
            }
            tx.commit();
            if (commandsReturned != null)
                System.out.printf("%s Command processed (%d ms)\n", system, (end - start));
            else {
                if (interactiveMode)
                    System.out.printf("%s Command not understood. Enter 'help' to get a list of commands.\n", system);
                else
                    throw new MCRUsageException("Command not understood: " + command);
            }
        } catch (Exception ex) {
            showException(ex);
            System.out.printf("%s Command failed. Performing transaction rollback...\n", system);
            try {
                tx.rollback();
            } catch (Exception ex2) {
                showException(ex2);
            }
            if (SKIP_FAILED_COMMAND) {
                saveFailedCommand(command);
            } else {
                saveQueue(command);
                if (!interactiveMode)
                    System.exit(1);
            }
        } finally {
            tx = MCRHIBConnection.instance().getSession().beginTransaction();
            MCRHIBConnection.instance().getSession().clear();
            tx.commit();
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
            System.out.println("  total: " + tn + " ms, average: " + (tn / num) + " ms, " + num + " invocations.");
        }
        System.out.println();
    }

    protected static void saveQueue(String lastCommand) {
        System.out.println(system);
        System.out.println(system + " The following command failed: ");
        System.out.println(system + " " + lastCommand);
        if (!commandQueue.isEmpty())
            System.out.printf("%s There are %s other commands still unprocessed.\n", system, commandQueue.size());
        else if (interactiveMode)
            return;
        commandQueue.add(0, lastCommand);
        saveCommandQueueToFile(commandQueue, "unprocessed-commands.txt");
    }

    private static void saveCommandQueueToFile(final Vector<String> queue, String fname) {
        System.out.println(system + " Writing unprocessed commands to file " + fname);

        try {
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fname)));
            for (String command : queue)
                pw.println(command);
            pw.close();
        } catch (IOException ex) {
            showException(ex);
        }
    }

    protected static void saveFailedCommand(String lastCommand) {
        System.out.println(system);
        System.out.println(system + " The following command failed: ");
        System.out.println(system + " " + lastCommand);
        if (!commandQueue.isEmpty())
            System.out.printf("%s There are %s other commands still unprocessed.\n", system, commandQueue.size());
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
        while ((line = br.readLine()) != null)
            System.out.printf("%04d: %s\n", i++, line);
        br.close();
        System.out.println();
    }

    /**
     * Shows details about an exception that occured during command processing
     * 
     * @param ex
     *            The exception that was catched while processing a command
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

        System.out.println(system);
        System.out.println(system + " Exception occured: " + ex.getClass().getName());
        System.out.println(system + " Exception message: " + ex.getLocalizedMessage());
        System.out.println(system);

        if (ex instanceof MCRActiveLinkException) {
            MCRActiveLinkException activeLinks = (MCRActiveLinkException) ex;
            StringBuffer msgBuf = new StringBuffer(system);
            msgBuf.append(" There are links active preventing the commit of work, see error message for details. The following links where affected:");
            Map links = activeLinks.getActiveLinks();
            Iterator destIt = links.keySet().iterator();
            String curDest;
            int count = 0;
            while (destIt.hasNext()) {
                count++;
                curDest = destIt.next().toString();
                List sources = (List) links.get(curDest);
                Iterator sourceIt = sources.iterator();
                while (sourceIt.hasNext()) {
                    msgBuf.append("\n\t").append(count).append(".) ").append(sourceIt.next().toString()).append("==>").append(curDest);
                }
            }
            msgBuf.append('\n');
            System.out.println(msgBuf.toString());
        }

        String trace = MCRException.getStackTraceAsString(ex);
        if (logger.isDebugEnabled())
            logger.debug(trace);
        else
            System.out.println(trace);

        if (ex instanceof MCRActiveLinkException) {
            MCRActiveLinkException activeLinks = (MCRActiveLinkException) ex;
            StringBuffer msgBuf = new StringBuffer();
            msgBuf.append("\nThere are links active preventing the commit of work, see error message for details. The following links where affected:");
            Map links = activeLinks.getActiveLinks();
            Iterator destIt = links.keySet().iterator();
            String curDest;
            while (destIt.hasNext()) {
                curDest = destIt.next().toString();
                logger.debug("Current Destination: " + curDest);
                List sources = (List) links.get(curDest);
                Iterator sourceIt = sources.iterator();
                while (sourceIt.hasNext()) {
                    msgBuf.append('\n').append(sourceIt.next().toString()).append("==>").append(curDest);
                }
            }
        }
        if (ex instanceof MCRException) {
            ex = ((MCRException) ex).getException();
            if (ex != null) {
                System.out.println(system);
                System.out.println(system + " This exception was caused by:");
                showException(ex);
            }
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
        System.out.println(system + " Reading commands from file " + file);

        String line;
        List<String> list = new ArrayList<String>();
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.startsWith("#") || (line.length() == 0)) {
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
        System.out.println(system + " The following " + knownCommands.size() + " commands can be used:");
        System.out.println(system);

        for (int i = 0; i < knownCommands.size(); i++) {
            System.out.println(system + " " + ((MCRCommand) knownCommands.get(i)).showSyntax());
        }
    }

    /**
     * Shows the help text of one or more commands.
     * 
     * @param com
     *            the command
     */
    public static void showCommandsHelp(String com) {
        boolean test = false;

        for (int i = 0; i < knownCommands.size(); i++) {
            if (((MCRCommand) knownCommands.get(i)).showSyntax().indexOf(com) != -1) {
                System.out.println(system + " " + ((MCRCommand) knownCommands.get(i)).showSyntax());
                System.out.println(system + "      " + ((MCRCommand) knownCommands.get(i)).getHelpText());
                System.out.println(system);
                test = true;
            }
        }

        if (!test) {
            System.out.println(system + " Unknown command.");
        }
    }

    /**
     * Executes simple shell commands from inside the command line interface and shows their output. This method implements commands entered beginning with
     * exclamation mark, like "! ls -l /temp"
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
     * The method print the current user.
     */
    public static void whoami() {
        System.out.println(system + " You are user " + session.getCurrentUserID());
    }

    /**
     * This command changes the user of the session context to a new user.
     * 
     * @param user
     *            the new user ID
     * @param password
     *            the password of the new user
     */
    public static void changeToUser(String user, String password) {
        if (userExists(user)) {
            System.out.println(system + " The old user ID is " + session.getCurrentUserID());

            if (org.mycore.user.MCRUserMgr.instance().login(user.trim(), password.trim())) {
                session.setCurrentUserID(user);
                session.setLoginTime();
                System.out.println(system + " The new user ID is " + session.getCurrentUserID());
            } else {
                String msg = "Wrong password, no changes of user ID in session context!";
                if (logger.isDebugEnabled())
                    logger.debug(msg);
                else
                    System.out.println(system + " " + msg);
            }
        }
    }

    /**
     * This command changes the user of the session context to a new user.
     * 
     * @param user
     *            the new user ID
     */
    public static void login(String user) {
        if (userExists(user)) {
            System.out.println(system + " The old user ID is " + session.getCurrentUserID());

            String password = "";

            do {
                System.out.print(system + " Enter the password for user " + user + ":> ");

                try {
                    password = console.readLine();
                } catch (IOException ex) {
                }
            } while ((password = password.trim()).length() == 0);

            changeToUser(user, password);
        }
    }

    private static boolean userExists(String user) {
        if (MCRUserMgr.instance().existUser(user))
            return true;
        System.out.println(system + " User does not exists: " + user);
        return false;
    }

    /**
     * Catches the output read from an input stream and prints it line by line on standard out. This is used to catch the stdout and stderr stream output when
     * executing an external shell command.
     */
    protected static void showOutput(InputStream in) throws IOException {
        int c;
        StringBuffer sb = new StringBuffer(1024);

        while ((c = in.read()) != -1) {
            sb.append((char) c);
        }

        System.out.println(system + " " + sb.toString());
    }

    public static void cancelOnError() {
        SKIP_FAILED_COMMAND = false;
    }

    public static void skipOnError() {
        SKIP_FAILED_COMMAND = true;
    }

    /**
     * Exits the command line interface. This method implements the "exit" and "quit" commands.
     */
    public static void exit() {
        System.out.println(system + " Session time: " + (System.currentTimeMillis() - session.getLoginTime()) + " ms");
        handleFailedCommands();
        System.exit(0);
    }
}
