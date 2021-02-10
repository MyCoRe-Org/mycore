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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
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
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.inject.MCRInjectorConfig;
import org.mycore.common.processing.MCRProcessableCollection;
import org.mycore.common.processing.MCRProcessableDefaultCollection;
import org.mycore.common.processing.MCRProcessableRegistry;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.frontend.ws.common.MCRWebsocketDefaultConfigurator;
import org.mycore.util.concurrent.processing.MCRProcessableExecutor;
import org.mycore.util.concurrent.processing.MCRProcessableFactory;
import org.mycore.util.concurrent.processing.MCRProcessableSupplier;
import org.mycore.webcli.cli.MCRWebCLICommandManager;
import org.mycore.webcli.flow.MCRCommandListProcessor;
import org.mycore.webcli.flow.MCRJSONSubscriber;
import org.mycore.webcli.flow.MCRLogEventProcessor;

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
    private final ReentrantLock lock;

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
        lock = new ReentrantLock();
        processCallable = new ProcessCallable(MCRSessionMgr.getCurrentSession(), session, lock);
    }

    public ReentrantLock getWebsocketLock() {
        return lock;
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
        return !(curFuture == null || curFuture.isFutureDone());
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
        if (knownCommands == null) {
            initializeCommands();
        }
    }

    public void changeWebSocketSession(Session webSocketSession) {
        this.processCallable.changeWebSocketSession(webSocketSession);
    }

    public void webSocketClosed() {
        this.processCallable.webSocketClosed();
    }

    public void stopLogging() {
        this.processCallable.stopLogging(true);
    }

    public void startLogging() {
        this.processCallable.startLogging(true);
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

        private final ReentrantLock lock;

        List<String> commands;

        Session webSocketSession;

        MCRSession session;

        Log4JGrabber logGrabber;

        String currentCommand;

        boolean continueIfOneFails;

        boolean stopLogs;

        private MCRLogEventProcessor logEventProcessor;

        private final SubmissionPublisher<List<String>> cmdListPublisher;

        private MCRCommandListProcessor cmdListProcessor;

        ProcessCallable(MCRSession session, Session webSocketSession, ReentrantLock lock) {
            this.commands = new ArrayList<>();
            this.session = session;
            this.lock = lock;
            this.stopLogs = false;
            this.webSocketSession = webSocketSession;
            this.logGrabber = new Log4JGrabber(MCRWebCLIContainer.class.getSimpleName() + session.getID(), null,
                PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
            this.logGrabber.start();
            startLogging(true);
            cmdListPublisher = new SubmissionPublisher<>(ForkJoinPool.commonPool(), 1);
            this.currentCommand = "";
            this.continueIfOneFails = false;
            startSendingCommandQueue();
        }

        public void stopLogging(boolean remember) {
            if (remember) {
                this.stopLogs = true;
            }
            this.logEventProcessor.close();
        }

        public void startLogging(boolean remember) {
            if (remember) {
                this.stopLogs = false;
            }
            if (this.stopLogs) {
                return;
            }
            if (logEventProcessor != null) {
                logEventProcessor.close();
            }
            MCRLogEventProcessor logEventProcessor = new MCRLogEventProcessor();
            MCRJSONSubscriber log2web = new MCRJSONSubscriber(webSocketSession, lock);
            logEventProcessor.subscribe(log2web);
            this.logEventProcessor = logEventProcessor;
            logGrabber.subscribe(logEventProcessor);
            if (logGrabber.isStopped()) {
                logGrabber.start();
            }
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
                    lock.lock();
                    webSocketSession.getBasicRemote().sendText(jObject.toString());
                } catch (IOException e) {
                    LOGGER.error("Cannot send message to client.", e);
                } finally {
                    lock.unlock();
                }
            }
        }

        public void clearCommandList() {
            this.commands.clear();
            cmdListPublisher.submit(commands);
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
         */
        private boolean processCommand(String command) {
            LOGGER.info("Processing command:'{}' ({} left)", command, commands.size());
            setCurrentCommand(command);
            long start = System.currentTimeMillis();
            session.beginTransaction();
            try {
                List<String> commandsReturned = null;
                for (List<MCRCommand> cmds : knownCommands.values()) {
                    // previous attempt to run command was successful
                    if (commandsReturned != null) {
                        break;
                    }
                    commandsReturned = runCommand(command, cmds);
                }
                updateKnownCommandsIfNeeded();
                session.commitTransaction();
                if (commandsReturned != null) {
                    LOGGER.info("Command processed ({} ms)", System.currentTimeMillis() - start);
                } else {
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
                MCREntityManagerProvider.getCurrentEntityManager().clear();
                session.commitTransaction();
            }
            return true;
        }

        private List<String> runCommand(String command, List<MCRCommand> commandList)
            throws IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException {
            List<String> commandsReturned = null;
            for (MCRCommand currentCommand : commandList) {
                commandsReturned = currentCommand.invoke(command, MCRClassTools.getClassLoader());
                if (commandsReturned != null) { // Command was executed
                    // Add commands to queue
                    if (commandsReturned.size() > 0) {
                        LOGGER.info("Queueing {} commands to process", commandsReturned.size());
                        commands.addAll(0, commandsReturned);
                        cmdListPublisher.submit(commands);
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
            String unprocessedCommandsFile = MCRConfiguration2.getStringOrThrow("MCR.WebCLI.UnprocessedCommandsFile");
            File file = new File(unprocessedCommandsFile);
            LOGGER.info("Writing unprocessed commands to file {}", file.getAbsolutePath());

            try {
                PrintWriter pw = new PrintWriter(file, Charset.defaultCharset().name());
                if (lastCommand != null) {
                    pw.println(lastCommand);
                }
                for (String command : commands.toArray(String[]::new)) {
                    pw.println(command);
                }
                if (failedQueue != null && !failedQueue.isEmpty()) {
                    for (String failedCommand : failedQueue) {
                        pw.println(failedCommand);
                    }
                }
                pw.close();
            } catch (IOException ex) {
                LOGGER.error("Cannot write to {}", file.getAbsolutePath(), ex);
            }
            clearCommandList();
        }

        protected boolean processCommands() throws IOException {
            final LoggerContext logCtx = (LoggerContext) LogManager.getContext(false);
            final AbstractConfiguration logConf = (AbstractConfiguration) logCtx.getConfiguration();
            LinkedList<String> failedQueue = new LinkedList<>();
            logGrabber.grabCurrentThread();
            // start grabbing logs of this thread
            logConf.getRootLogger().addAppender(logGrabber, logConf.getRootLogger().getLevel(), null);
            // register session to MCRSessionMgr
            MCRSessionMgr.setCurrentSession(session);
            Optional<HttpSession> httpSession = Optional
                .ofNullable((HttpSession) webSocketSession.getUserProperties()
                    .get(MCRWebsocketDefaultConfigurator.HTTP_SESSION));
            int sessionTime = httpSession.map(HttpSession::getMaxInactiveInterval).orElse(-1);
            httpSession.ifPresent(s -> s.setMaxInactiveInterval(-1));
            try {
                while (!commands.isEmpty()) {
                    String command = commands.remove(0);
                    cmdListPublisher.submit(commands);
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
                logGrabber.stop();
                logConf.removeAppender(logGrabber.getName());
                try {
                    if (webSocketSession.isOpen()) {
                        LogManager.getLogger().info("Close session {}", webSocketSession::getId);
                        webSocketSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Done"));
                    }
                } finally {
                    httpSession.ifPresent(s -> s.setMaxInactiveInterval(sessionTime));
                    // release session
                    MCRSessionMgr.releaseCurrentSession();
                }
            }
        }

        public void startSendingCommandQueue() {
            MCRCommandListProcessor commandListProcessor = new MCRCommandListProcessor();
            commandListProcessor.subscribe(new MCRJSONSubscriber(this.webSocketSession, lock));
            cmdListPublisher.subscribe(commandListProcessor);
            this.cmdListProcessor = commandListProcessor;
            cmdListPublisher.submit(commands);
        }

        public void changeWebSocketSession(Session webSocketSession) {
            webSocketClosed();
            this.webSocketSession = webSocketSession;
            startLogging(false);
            sendCurrentCommand();
            startSendingCommandQueue();
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
                    lock.lock();
                    webSocketSession.getBasicRemote().sendText(jObject.toString());
                } catch (IOException ex) {
                    LOGGER.error("Cannot send message to client.", ex);
                } finally {
                    lock.unlock();
                }
            }
        }

        @Override
        public String toString() {
            return this.commands.stream().findFirst().orElse("no active command");
        }

        public void webSocketClosed() {
            stopLogging(false);
            cmdListProcessor.close();
        }
    }

    private static class Log4JGrabber extends AbstractAppender implements Flow.Publisher<LogEvent> {

        private SubmissionPublisher<LogEvent> publisher;

        public String webCLIThread;

        private static int MAX_BUFFER = 10000;

        private List<Flow.Subscriber<? super LogEvent>> subscribers;

        protected Log4JGrabber(String name, Filter filter, Layout<? extends Serializable> layout,
            final boolean ignoreExceptions, final Property[] properties) {
            super(name, filter, layout, ignoreExceptions, properties);
        }

        @Override
        public void start() {
            super.start();
            if (this.publisher != null && !this.publisher.isClosed()) {
                stop();
            }
            this.publisher = new SubmissionPublisher<>(ForkJoinPool.commonPool(), MAX_BUFFER);
            if (subscribers != null) {
                subscribers.forEach(publisher::subscribe);
            }
            grabCurrentThread();
        }

        @Override
        public void stop() {
            while (publisher.estimateMaximumLag() > 0) {
                if (publisher.getExecutor() instanceof ForkJoinPool) {
                    ((ForkJoinPool) publisher.getExecutor()).awaitQuiescence(1, TimeUnit.SECONDS);
                } else {
                    Thread.yield();
                }
            }
            this.subscribers = publisher.getSubscribers();
            super.stop();
            this.publisher.close();
        }

        public void grabCurrentThread() {
            this.webCLIThread = Thread.currentThread().getName();
        }

        @Override
        public void append(LogEvent event) {
            if (webCLIThread.equals(event.getThreadName())) {
                publisher.submit(event);
            }
        }

        @Override
        public void subscribe(Flow.Subscriber<? super LogEvent> subscriber) {
            publisher.subscribe(subscriber);
        }
    }
}
