/**
 * 
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
package org.mycore.services.mbeans;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Transaction;

import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRUsageException;
import org.mycore.frontend.cli.MCRCommand;

public abstract class MCRCommandWrapperMBean {

    private LinkedList<String> commandQueue;

    private List<MCRCommand> knownCommands;

    public MCRCommandWrapperMBean() {
        super();
        commandQueue = new LinkedList<String>();
        knownCommands = getCommands();
    }

    abstract protected List<MCRCommand> getCommands();

    abstract protected Logger getLogger();

    abstract protected String getName();

    /**
     * method mainly copied from CLI class
     * 
     * @param command
     * @return true if command processed successfully
     */
    private boolean processCommand(String command) {
        getLogger().info(getName() + " Processing command:'" + command + "'");
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
                        getLogger().info(getName() + " Queueing " + commandsReturned.size() + " commands to process");
                        commandQueue.addAll(0, commandsReturned);
                    }

                    break;
                }
            }
            tx.commit();
            if (commandsReturned != null)
                getLogger().info(getName() + " Command processed (" + (System.currentTimeMillis() - start) + " ms)");
            else {
                throw new MCRUsageException("Command not understood: " + command);
            }
        } catch (Exception ex) {
            getLogger().error(getName() + " Command '" + command + "' failed. Performing transaction rollback...", ex);
            try {
                tx.rollback();
            } catch (Exception ex2) {
                getLogger().error(getName() + "Error while perfoming rollback for command '" + command + "'!", ex2);
            }
            saveQueue(command);
            return false;
        }
        return true;
    }

    protected void addCommand(String command) {
        commandQueue.add(command);
    }

    protected void saveQueue(String lastCommand) {
        getLogger().warn(getName() + " The following command failed: '" + lastCommand + "'");
        if (!commandQueue.isEmpty())
            getLogger().warn(getName() + "There are " + commandQueue.size() + " other commands still unprocessed.");

        File file = new File(getName() + "-unprocessed-commands.txt");
        getLogger().warn(getName() + " Writing unprocessed commands to file " + file.getAbsolutePath());

        try {
            PrintWriter pw = new PrintWriter(new FileWriter(file));
            pw.println(lastCommand);
            for (String command : commandQueue)
                pw.println(command);
            pw.close();
        } catch (IOException ex) {
            getLogger().error("Cannot write to " + file.getAbsolutePath(), ex);
        }
    }

    protected boolean processCommands() {
        while (!commandQueue.isEmpty()) {
            String command = commandQueue.poll();
            if (!processCommand(command)) {
                return false;
            }
        }
        return true;
    }

}