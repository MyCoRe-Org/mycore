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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.ifs2.MCRContent;

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
    
    static Logger logger = Logger.getLogger(MCRCommandLineInterface.class);

    /** The name of the system */
    private static String system = null;

    /** A queue of commands waiting to be executed */
    protected static Vector<String> commandQueue = new Vector<String>();

    protected static Vector<String> failedCommands = new Vector<String>();

    protected static ConcurrentLinkedQueue<Number> benchList = new ConcurrentLinkedQueue<Number>();

    private static boolean interactiveMode = true;

    private static boolean SKIP_FAILED_COMMAND = false;

    private static MCRKnownCommands knownCommands;
    
    /**
     * The main method that either shows up an interactive command prompt or
     * reads a file containing a list of commands to be processed
     */
    public static void main(String[] args) {
        system = MCRConfiguration.instance().getString("MCR.CommandLineInterface.SystemName", "MyCoRe") + ":";
        initSession();

        System.out.println();
        output("Command Line Interface.");
        output("");
        output("Initializing...");

        knownCommands = new MCRKnownCommands();

        output("Initialization done.");
        output("Type 'help' to list all commands!");
        output("");

        readCommandFromArguments(args);

        String command = null;
        String firstCommand = null;

        MCRCommandPrompt prompt = new MCRCommandPrompt( system );
        while (true) {
            if (commandQueue.isEmpty()) {
                if (interactiveMode) {
                    command = prompt.readCommand();
                } else {
                    if (firstCommand != null && shouldSaveRuntimeStatistics()) {
                        saveMillis(firstCommand);
                    }
                    exit();
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
     * Processes a command entered by searching a matching command in the list
     * of known commands and executing its method.
     * 
     * @param command
     *            The command string to be processed
     */
    protected static void processCommand(String command) {

        String invokedCommand = null;
        MCRSession session = MCRSessionMgr.getCurrentSession();

        try {
            
            session.beginTransaction();
            List<String> commandsReturned = knownCommands.invokeCommand(command);
            session.commitTransaction();
            
            if (commandsReturned != null) // Command was executed
            {
                // Add commands to queue
                if (commandsReturned.size() > 0) {
                    System.out.println(system + " Queueing " + commandsReturned.size() + " commands to process");

                    for (int i = 0; i < commandsReturned.size(); i++) {
                        commandQueue.insertElementAt(commandsReturned.get(i), i);
                    }
                }
            }
            else {
                if (interactiveMode) {
                    System.out.printf("%s Command not understood. Enter 'help' to get a list of commands.\n", system);
                } else {
                    throw new MCRUsageException("Command not understood: " + command);
                }
            }
        } catch (Exception ex) {
            MCRCLIExceptionHandler.handleException(ex);

            System.out.printf("%s Command failed. Performing transaction rollback...\n", system);
            if (session.isTransactionActive()) {
                try {
                    session.rollbackTransaction();
                } catch (Exception ex2) {
                    MCRCLIExceptionHandler.handleException(ex2);
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
            if (!session.isTransactionActive()) {
                session.beginTransaction();
                MCRHIBConnection.instance().getSession().clear();
                session.commitTransaction();
            }
        }
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
            MCRCLIExceptionHandler.handleException(ex);
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
     * Catches the output read from an input stream and prints it line by line
     * on standard out. This is used to catch the stdout and stderr stream
     * output when executing an external shell command.
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

    public static void saveMillis(String fileBaseName) {
        try {
            PrintStream fout = new PrintStream(fileBaseName + ".dat");

            for (int i = 1; !benchList.isEmpty(); i++) {
                fout.printf("%d %d\n", i, benchList.poll().intValue());
            }
            fout.close();
        } catch (IOException ex) {
            MCRCLIExceptionHandler.handleException(ex);
        }
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
