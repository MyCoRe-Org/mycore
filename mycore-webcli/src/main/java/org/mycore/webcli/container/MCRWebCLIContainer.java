/**
 * $RCSfile$ $Revision$ $Date: 2011-03-23 13:37:07 +0100 (Wed, 23 Mar
 * 2011) $ This file is part of ** M y C o R e ** Visit our homepage at
 * http://www.mycore.de/ for details. This program is free software; you can use
 * it, redistribute it and / or modify it under the terms of the GNU General
 * Public License (GPL) as published by the Free Software Foundation; either
 * version 2 of the License or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program, normally in the file license.txt. If not, write to the Free Software
 * Foundation Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307 USA
 **/
package org.mycore.webcli.container;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.websocket.Session;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.inject.MCRInjectorConfig;
import org.mycore.common.processing.MCRProcessableCollection;
import org.mycore.common.processing.MCRProcessableDefaultCollection;
import org.mycore.common.processing.MCRProcessableRegistry;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.util.concurrent.processing.MCRProcessableExecutor;
import org.mycore.util.concurrent.processing.MCRProcessableFactory;
import org.mycore.util.concurrent.processing.MCRProcessableSupplier;
import org.mycore.webcli.cli.MCRWebCLICommandManager;
import org.mycore.webcli.observable.CommandListObserver;
import org.mycore.webcli.observable.LogEventDequeObserver;
import org.mycore.webcli.observable.ObservableCommandList;
import org.mycore.webcli.observable.ObservableLogEventDeque;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Is a wrapper class around command execution. Commands will be {@link #addCommand(String) queued} and executed in a
 * seperate thread.
 *
 * @author Thomas Scheffler (yagee)
 * @author Michel Buechner (mcrmibue)
 * @since 2.0
 */
public class MCRWebCLIContainer {
    MCRProcessableSupplier<Boolean> curFuture;

    private static Map<String, List<MCRCommand>> knownCommands;

    private final ProcessCallable processCallable;

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRProcessableExecutor EXECUTOR;

    private static final MCRProcessableCollection PROCESSABLE_COLLECTION;

    static {
        PROCESSABLE_COLLECTION = new MCRProcessableDefaultCollection("Web CLI");
        MCRProcessableRegistry registry = MCRInjectorConfig.injector().getInstance(MCRProcessableRegistry.class);
        registry.register(PROCESSABLE_COLLECTION);

        ExecutorService service = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "WebCLI"));
        EXECUTOR = MCRProcessableFactory.newPool(service, PROCESSABLE_COLLECTION);
    }

    /**
     * Will instantiate this container with a list of supported commands.
     *
     * @param session
     *            the current Session(Websocket) of the user using the gui.
     */
    public MCRWebCLIContainer(Session session) {
        processCallable = new ProcessCallable(MCRSessionMgr.getCurrentSession(), session);
    }

    /**
     * Adds this <code>cmd</code> to the current command queue. The thread
     * executing the commands will be started automatically if the queue was
     * previously empty.
     *
     * @param cmd
     *            a valid String representation of a
     *            {@link #MCRWebCLIContainer(Session) known}
     *            <code>MCRCommand</code>
     */
    public void addCommand(String cmd) {
        LOGGER.info("appending command: {}", cmd);
        processCallable.commands.add(cmd);
        if (!isRunning()) {
            curFuture = EXECUTOR.submit(processCallable);
        }
    }

    /**
     * Returns the status of the command execution thread.
     *
     * @return true if the thread is running
     */
    public boolean isRunning() {
        return !(curFuture == null || curFuture.isDone());
    }

    public static JsonObject getKnownCommands() {
        updateKnownCommandsIfNeeded();
        JsonObject commandsJSON = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        commandsJSON.add("commands", jsonArray);
        for (Map.Entry<String, List<MCRCommand>> entry : knownCommands.entrySet()) {
            //sort commands for display only (fix for MCR-1594)
            JsonArray commands = entry.getValue().stream()
                .sorted((commandA, commandB) -> commandA.getSyntax().compareToIgnoreCase(commandB.getSyntax()))
                .map(cmd -> {
                    JsonObject command = new JsonObject();
                    command.add("command", new JsonPrimitive(cmd.getSyntax()));
                    command.add("help", new JsonPrimitive(cmd.getHelpText()));
                    return command;
                }).collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
            JsonObject item = new JsonObject();
            item.addProperty("name", entry.getKey());
            item.add("commands", commands);
            jsonArray.add(item);
        }
        return commandsJSON;
    }

    protected static void initializeCommands() {
        if (knownCommands == null) {
            knownCommands = new TreeMap<>();
            knownCommands.putAll(new MCRWebCLICommandManager().getCommandsMap());
        }
    }

    private static void updateKnownCommandsIfNeeded() {
        if (knownCommands == null)
            initializeCommands();
    }

    public void changeWebSocketSession(Session webSocketSession) {
        this.processCallable.changeWebSocketSession(webSocketSession);
    }

    public void stopLogging() {
        this.processCallable.stopLogging();
    }

    public void startLogging() {
        this.processCallable.startLogging();
    }

    public void setContinueIfOneFails(boolean con) {
        this.processCallable.setContinueIfOneFails(con);
    }

    public void setContinueIfOneFails(boolean con, boolean sendMessage) {
        this.processCallable.setContinueIfOneFails(con, sendMessage);
    }

    public void clearCommandList() {
        this.processCallable.clearCommandList();
    }

    private static class ProcessCallable implements Callable<Boolean> {

        ObservableLogEventDeque logs;

        ObservableCommandList commands;

        Session webSocketSession;

        MCRSession session;

        Log4JGrabber logGrabber;

        CommandListObserver commandListObserver;

        LogEventDequeObserver logEventQueueObserver;

        String currentCommand;

        boolean continueIfOneFails;

        public ProcessCallable(MCRSession session, Session webSocketSession) {
            this.commands = new ObservableCommandList();
            this.session = session;
            this.webSocketSession = webSocketSession;
            this.logs = new ObservableLogEventDeque();
            this.logGrabber = new Log4JGrabber(MCRWebCLIContainer.class.getSimpleName() + session.getID(), null,
                PatternLayout.createDefaultLayout());
            this.logGrabber.start();

            this.commandListObserver = new CommandListObserver(commands, webSocketSession);
            commands.addObserver(commandListObserver);
            this.logEventQueueObserver = new LogEventDequeObserver(logs, webSocketSession);
            logs.addObserver(logEventQueueObserver);
            this.currentCommand = "";
            this.continueIfOneFails = false;
        }

        public void stopLogging() {
            this.logEventQueueObserver.stopSendMessages();
        }

        public void startLogging() {
            this.logEventQueueObserver.startSendMessages();
        }

        public void setContinueIfOneFails(boolean con) {
            setContinueIfOneFails(con, false);
        }

        public void setContinueIfOneFails(boolean con, boolean sendMessage) {
            this.continueIfOneFails = con;
            if (sendMessage) {
                JsonObject jObject = new JsonObject();
                jObject.addProperty("type", "continueIfOneFails");
                jObject.addProperty("value", con);
                try {
                    webSocketSession.getBasicRemote().sendText(jObject.toString());
                } catch (IOException e) {
                    LOGGER.error("Cannot send message to client.", e);
                }
            }
        }

        public void clearCommandList() {
            this.commands.clear();
            setCurrentCommand("");
        }

        public Boolean call() throws Exception {
            return processCommands();
        }

        /**
         * method mainly copied from CLI class
         *
         * @param command
         * @return true if command processed successfully
         * @throws IOException
         */
        private boolean processCommand(String command) throws IOException {
            LOGGER.info("Processing command:'{}' ({} left)", command, commands.size());
            setCurrentCommand(command);
            long start = System.currentTimeMillis();
            session.beginTransaction();
            try {
                List<String> commandsReturned = null;
                for (List<MCRCommand> cmds : knownCommands.values()) {
                    // previous attempt to run command was successful
                    if (commandsReturned != null)
                        break;
                    commandsReturned = runCommand(command, cmds);
                }
                updateKnownCommandsIfNeeded();
                session.commitTransaction();
                if (commandsReturned != null)
                    LOGGER.info("Command processed ({} ms)", System.currentTimeMillis() - start);
                else {
                    throw new MCRUsageException("Command not understood: " + command);
                }
            } catch (Exception ex) {
                LOGGER.error("Command '{}' failed. Performing transaction rollback...", command, ex);
                try {
                    session.rollbackTransaction();
                } catch (Exception ex2) {
                    LOGGER.error("Error while perfoming rollback for command '{}'!", command, ex2);
                }
                if (!continueIfOneFails) {
                    saveQueue(command, null);
                }
                return false;
            } finally {
                session.beginTransaction();
                MCRHIBConnection.instance().getSession().clear();
                session.commitTransaction();
            }
            return true;
        }

        private List<String> runCommand(String command, List<MCRCommand> commandList)
            throws IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException {
            List<String> commandsReturned = null;
            for (MCRCommand currentCommand : commandList) {
                commandsReturned = currentCommand.invoke(command, this.getClass().getClassLoader());
                if (commandsReturned != null) // Command was executed
                {
                    // Add commands to queue
                    if (commandsReturned.size() > 0) {
                        LOGGER.info("Queueing {} commands to process", commandsReturned.size());
                        commands.addAll(0, commandsReturned);
                    }

                    break;
                }
            }
            return commandsReturned;
        }

        protected void saveQueue(String lastCommand, LinkedList<String> failedQueue) {
            // lastCommand is null if work is not stopped at first error
            if (lastCommand == null) {
                LOGGER.error("Some commands failed.");
            } else {
                LOGGER.printf(Level.ERROR, "The following command failed: '%s'", lastCommand);
            }
            if (!commands.isEmpty()) {
                LOGGER.printf(Level.INFO, "There are %d other commands still unprocessed.", commands.size());
            }
            String unprocessedCommandsFile = MCRConfiguration.instance()
                .getString("MCR.WebCLI.UnprocessedCommandsFile");
            File file = new File(unprocessedCommandsFile);
            LOGGER.info("Writing unprocessed commands to file {}", file.getAbsolutePath());

            try {
                PrintWriter pw = new PrintWriter(file, Charset.defaultCharset().name());
                if (lastCommand != null) {
                    pw.println(lastCommand);
                }
                for (String command : commands.getCopyAsArrayList())
                    pw.println(command);
                if (failedQueue != null && !failedQueue.isEmpty()) {
                    for (String failedCommand : failedQueue) {
                        pw.println(failedCommand);
                    }
                }
                pw.close();
            } catch (IOException ex) {
                LOGGER.error("Cannot write to {}", file.getAbsolutePath(), ex);
            }
            setCurrentCommand("");
            commands.clear();
        }

        protected boolean processCommands() throws IOException {
            final LoggerContext logCtx = (LoggerContext) LogManager.getContext(false);
            final AbstractConfiguration logConf = (AbstractConfiguration) logCtx.getConfiguration();
            LinkedList<String> failedQueue = new LinkedList<>();
            logGrabber.grabCurrentThread();
            logGrabber.setLogEventList(logs);
            // start grabbing logs of this thread
            logConf.getRootLogger().addAppender(logGrabber, logConf.getRootLogger().getLevel(), null);
            // register session to MCRSessionMgr
            MCRSessionMgr.setCurrentSession(session);
            try {
                while (!commands.isEmpty()) {
                    String command = commands.remove(0);
                    if (!processCommand(command)) {
                        if (!continueIfOneFails) {
                            return false;
                        }
                        failedQueue.add(command);
                    }
                }
                if (failedQueue.isEmpty()) {
                    setCurrentCommand("");
                    return true;
                } else {
                    saveQueue(null, failedQueue);
                    return false;
                }
            } finally {
                // stop grabbing logs of this thread
                logConf.removeAppender(logGrabber.getName());
                // release session
                MCRSessionMgr.releaseCurrentSession();
            }
        }

        public void changeWebSocketSession(Session webSocketSession) {
            this.webSocketSession = webSocketSession;
            this.logEventQueueObserver.changeSession(webSocketSession);
            this.commandListObserver.changeSession(webSocketSession);
            sendCurrentCommand();
        }

        private void setCurrentCommand(String command) {
            this.currentCommand = command;
            sendCurrentCommand();
        }

        private void sendCurrentCommand() {
            JsonObject jObject = new JsonObject();
            jObject.addProperty("type", "currentCommand");
            jObject.addProperty("return", currentCommand);
            if (webSocketSession.isOpen()) {
                try {
                    webSocketSession.getBasicRemote().sendText(jObject.toString());
                } catch (IOException ex) {
                    LOGGER.error("Cannot send message to client.", ex);
                }
            }
        }

        @Override
        public String toString() {
            if (this.commands.isEmpty()) {
                return "no active command";
            }
            return this.commands.getCopyAsArrayList().get(0);
        }

    }

    private static class Log4JGrabber extends AbstractAppender {

        protected Log4JGrabber(String name, Filter filter, Layout<? extends Serializable> layout) {
            super(name, filter, layout);
        }

        @Override
        public void start() {
            super.start();
            grabCurrentThread();
        }

        @Override
        public void stop() {
            super.stop();
            logEvents.clear();
        }

        public String webCLIThread;

        public ObservableLogEventDeque logEvents;

        public void grabCurrentThread() {
            this.webCLIThread = Thread.currentThread().getName();
        }

        public void setLogEventList(ObservableLogEventDeque logs) {
            logEvents = logs;
        }

        @Override
        public void append(LogEvent event) {
            if (webCLIThread.equals(event.getThreadName())) {
                logEvents.add(event);
            }
        }

    }
}
