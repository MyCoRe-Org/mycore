package org.mycore.webcli.cli.command;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.mycore.common.MCRClassTools;
import org.mycore.webcli.cli.MCRCommandPool;
import org.mycore.webcli.cli.MCRJarTools;

public class MCRAddCommands extends MCRWebCLICommand {
    private static final Logger LOGGER = Logger.getLogger(MCRAddCommands.class);

    @Override
    protected String commandName() {
        return "add commands";
    }

    @Override
    protected String helpText() {
        return "add command [pathToJars] - add CLI commands to system";
    }

    /**
     * Checks cmdPath for jar files and loads 
     * commands into the command pool.
     * 
     * @param path to the jar files
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws ClassNotFoundException 
     * @throws IOException 
     * @throws MalformedURLException 
     */
    public static void cmdAdd(String path) throws MalformedURLException, IOException, ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        File file = new File(path);

        if (file.isDirectory()) {
            File[] jarFiles = MCRJarTools.listJarFiles(file);
            for (File jarFile : jarFiles) {
                addCommandFromJar(jarFile);
            }
        } else {
            addCommandFromJar(file);
        }
    }

    private static void addCommandFromJar(File file) throws IOException, MalformedURLException, ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        ArrayList<String> classesFromJar = MCRJarTools.getClassesFromJar(file);

        for (String className : classesFromJar) {
            Object cmd = MCRClassTools.loadClassFromURL(file, className);
            if (cmd instanceof MCRWebCLICommand) {
                LOGGER.info("Adding command to pool: " + cmd);
                MCRCommandPool.instance().addCommand((MCRWebCLICommand) cmd);
            }
        }
    }
}
