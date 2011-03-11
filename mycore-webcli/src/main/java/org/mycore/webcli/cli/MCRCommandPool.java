package org.mycore.webcli.cli;

import java.util.ArrayList;
import java.util.HashMap;

import org.mycore.frontend.cli.MCRCommand;
import org.mycore.webcli.cli.command.MCRWebCLICommand;

public class MCRCommandPool {
    private static MCRCommandPool instance;
    
    private long lastModified;
    
    private HashMap<String, MCRWebCLICommand> cmdMap = new HashMap<String, MCRWebCLICommand>();

    private MCRCommandPool() {
        touch();
    }

    private void touch() {
        lastModified=System.currentTimeMillis();
    }

    public static MCRCommandPool instance() {
        if (instance == null)
            instance = new MCRCommandPool();

        return instance;
    }

    /**
     * Adds a command to the command pool
     * 
     * @param command
     * @return true if the command does not exist in the command pool and could added successfully
     *         false if the command exists in the command pool and will not be added
     */
    public boolean addCommand(MCRWebCLICommand command) {
        String commandName = command.toString();
        if (cmdMap.containsKey(commandName)) {
            return false;
        }
        cmdMap.put(command.toString(), command);
        touch();
        return true;
    }

    public MCRWebCLICommand removeCommand(String commandName) {
        touch();
        return cmdMap.remove(commandName);
    }
    
    public ArrayList<MCRCommand> getPossibleCommands() {
        return new ArrayList<MCRCommand>(cmdMap.values());
    }

    public long getLastModified() {
        return lastModified;
    }
}
