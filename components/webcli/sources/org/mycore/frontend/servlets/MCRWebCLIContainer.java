/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.frontend.servlets;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpSession;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.hibernate.Transaction;

import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;
import org.mycore.frontend.cli.MCRCommand;

/**
 * Is a wrapper class around command execution.
 * 
 * Commands will be {@link #addCommand(String) queued} and executed in a
 * seperate thread. All logging events in that thread are grabbed and can be
 * retrieved by the {@link #getLogs() getLogs} method.
 * 
 * @author Thomas Scheffler (yagee)
 * @since 2.0
 */
class MCRWebCLIContainer {
    Future<Boolean> curFuture;

    private static List<MCRCommand> knownCommands;

    private final ProcessCallable processCallable;

    private static final Logger LOGGER = Logger.getLogger(MCRWebCLIContainer.class);

    private static final ExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Will instantiate this container with a list of supported commands.
     * 
     * @param knownCommands
     *            commands that are supported by the web gui.
     * @param session
     *            the current HttpSession of the usere using the gui.
     * 
     */
    public MCRWebCLIContainer(List<MCRCommand> knownCommands, HttpSession session) {
        MCRWebCLIContainer.knownCommands = knownCommands;
        processCallable = new ProcessCallable(MCRSessionMgr.getCurrentSession(), session);
    }

    /**
     * Adds this <code>cmd</code> to the current command queue.
     * 
     * The thread executing the commands will be started automatically if the
     * queue was previously empty.
     * 
     * @param cmd a valid String representation of a {@link #MCRWebCLIContainer(List, HttpSession) known} <code>MCRCommand</code>
     */
    public void addCommand(String cmd) {
        LOGGER.info("appending command: " + cmd);
        getCommandQueue().add(cmd);
        if (!isRunning()) {
            curFuture = executor.submit(processCallable);
        }
    }

    /**
     * Gets the current command queue.
     * @return the queue of commands yet to be processed
     */
    public LinkedList<String> getCommandQueue() {
        return processCallable.commands;
    }

    /**
     * Returns the status of the command execution thread.
     * @return true if the thread is running
     */
    public boolean isRunning() {
        return !(curFuture == null || curFuture.isDone());
    }

    /**
     * Returns all logs that were grabbed in the command execution thread.
     * 
     * This method is backed by a queue that will be empty after the method returns. 
     * @return
     *  {"logs": {<br/>
     *  &#160;&#160;&#160;&#160;"logLevel": <code>logLevel</code>,<br/>
     *  &#160;&#160;&#160;&#160;"message": <code>message</code>,<br/>
     *  &#160;&#160;&#160;&#160;"exception": <code>exception</code><br/>
     *  }}
     */
    public JSONObject getLogs() {
        JSONObject json = new JSONObject();
        json.put("logs", getJSONLogs(processCallable.logs));
        return json;
    }

    private static JSONArray getJSONLogs(Queue<LoggingEvent> events) {
        JSONArray array = new JSONArray();
        while (!events.isEmpty()) {
            LoggingEvent event = events.poll();
            JSONObject json = new JSONObject();
            json.put("logLevel", event.getLevel().toString());
            json.put("message", event.getRenderedMessage());
            String exception = null;
            if (event.getThrowableInformation() != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                event.getThrowableInformation().getThrowable().printStackTrace(pw);
                pw.close();
                exception = sw.toString();
            }
            json.put("exception", exception);
            json.put("time", event.timeStamp);
            array.put(json);
        }
        return array;
    }

    private static class ProcessCallable implements Callable<Boolean> {

        ConcurrentLinkedQueue<LoggingEvent> logs;

        LinkedList<String> commands;

        HttpSession hsession;

        MCRSession session;

        Log4JGrabber logGrabber;

        public ProcessCallable(MCRSession session, HttpSession hsession) {
            this.commands = new LinkedList<String>();
            this.session = session;
            this.hsession = hsession;
            this.logs = new MaxConcurentLinkedQueue<LoggingEvent>(1000);
            this.logGrabber = new Log4JGrabber();
        }

        public Boolean call() throws Exception {
            return processCommands(true);
        }

        /**
         * method mainly copied from CLI class
         * 
         * @param command
         * @return true if command processed successfully
         * @throws IOException
         */
        private boolean processCommand(String command, boolean continueIfOneFails) throws IOException {
            LOGGER.info("Processing command:'" + command + "' (" + commands.size() + " left)");
            long start = System.currentTimeMillis();
            Transaction tx = MCRHIBConnection.instance().getSession().beginTransaction();
            List<String> commandsReturned = null;
            try {
                for (MCRCommand currentCommand : knownCommands) {
                    commandsReturned = currentCommand.invoke(command, this.getClass().getClassLoader());
                    if (commandsReturned != null) // Command was executed
                    {
                        // Add commands to queue
                        if (commandsReturned.size() > 0) {
                            LOGGER.info("Queueing " + commandsReturned.size() + " commands to process");
                            commands.addAll(0, commandsReturned);
                        }

                        break;
                    }
                }
                tx.commit();
                if (commandsReturned != null)
                    LOGGER.info("Command processed (" + (System.currentTimeMillis() - start) + " ms)");
                else {
                    throw new MCRUsageException("Command not understood: " + command);
                }
            } catch (Exception ex) {
                LOGGER.error("Command '" + command + "' failed. Performing transaction rollback...", ex);
                try {
                    tx.rollback();
                } catch (Exception ex2) {
                    LOGGER.error("Error while perfoming rollback for command '" + command + "'!", ex2);
                }
                if (!continueIfOneFails) {
                    saveQueue(command);
                }
                return false;
            } finally {
                tx = MCRHIBConnection.instance().getSession().beginTransaction();
                MCRHIBConnection.instance().getSession().clear();
                tx.commit();
            }
            return true;
        }

        protected void saveQueue(String lastCommand) throws IOException {
            // lastCommand is null if work is not stopped at first error
            if (lastCommand == null) {
                LOGGER.error("Some commands failed.", null);
            } else {
                LOGGER.error("The following command failed: '" + lastCommand + "'", null);
            }
            if (!commands.isEmpty())
                LOGGER.info("There are " + commands.size() + " other commands still unprocessed.");

            File file = new File(MCRWebCLIServlet.class.getSimpleName() + "-unprocessed-commands.txt");
            LOGGER.info("Writing unprocessed commands to file " + file.getAbsolutePath());

            try {
                PrintWriter pw = new PrintWriter(new FileWriter(file));
                if (lastCommand != null) {
                    pw.println(lastCommand);
                }
                for (String command : commands)
                    pw.println(command);
                pw.close();
            } catch (IOException ex) {
                LOGGER.error("Cannot write to " + file.getAbsolutePath(), ex);
            }
        }

        protected boolean processCommands(boolean continueIfOneFailes) throws IOException {
            LinkedList<String> failedQueue = new LinkedList<String>();
            int maxSessionTime = hsession.getMaxInactiveInterval();
            logGrabber.grabCurrentThread();
            logGrabber.setLogEventList(logs);
            // start grabbing logs of this thread
            Logger.getRootLogger().addAppender(logGrabber);
            // register session to MCRSessionMgr
            MCRSessionMgr.setCurrentSession(session);
            try {
                // don't let session expire
                hsession.setMaxInactiveInterval(-1);
                while (!commands.isEmpty()) {
                    String command = commands.poll();
                    if (!processCommand(command, continueIfOneFailes)) {
                        if (!continueIfOneFailes) {
                            return false;
                        }
                        failedQueue.add(command);
                    }
                }
                if (failedQueue.isEmpty()) {
                    return true;
                } else {
                    saveQueue(null);
                    return false;
                }
            } finally {
                // restore old session expire time
                hsession.setMaxInactiveInterval(maxSessionTime);
                // stop grabbing logs of this thread
                Logger.getRootLogger().removeAppender(logGrabber);
                // release session
                MCRSessionMgr.releaseCurrentSession();
            }
        }

    }

    private static class Log4JGrabber extends AppenderSkeleton {

        public String webCLIThread;

        public Queue<LoggingEvent> logEvents;

        public Log4JGrabber() {
            grabCurrentThread();
        }

        public void grabCurrentThread() {
            this.webCLIThread = Thread.currentThread().getName();
        }

        public void setLogEventList(Queue<LoggingEvent> logs) {
            logEvents = logs;
        }

        @Override
        protected void append(LoggingEvent e) {
            if (webCLIThread.equals(e.getThreadName())) {
                logEvents.add(e);
            }
        }

        public void close() {
        }

        public boolean requiresLayout() {
            return false;
        }
    }

    private static class MaxConcurentLinkedQueue<E> extends ConcurrentLinkedQueue<E> {

        private static final long serialVersionUID = 705154376017755038L;

        AtomicInteger size;

        int maxSize;

        public MaxConcurentLinkedQueue(int maxSize) {
            super();
            this.size = new AtomicInteger();
            this.maxSize = maxSize;
        }

        @Override
        public boolean add(E e) {
            boolean value = super.add(e);
            size.incrementAndGet();
            while (size.get() > maxSize) {
                poll();
            }
            return value;
        }

        @Override
        public E poll() {
            size.decrementAndGet();
            return super.poll();
        }

    }
}
