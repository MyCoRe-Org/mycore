package org.mycore.webcli.cli.command;

import org.apache.log4j.Logger;

public class MCRHelloWorldCmd extends MCRWebCLICommand {
    private static Logger LOGGER = Logger.getLogger(MCRHelloWorldCmd.class);
    
    @Override
    protected String commandName() {
        return "hello";
    }

    @Override
    protected String helpText() {
        // TODO Auto-generated method stub
        return null;
    }

    public static void cmdHello(){
        LOGGER.info("Hello World!");
    }
}
