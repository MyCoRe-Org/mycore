/**
 * 
 */
package org.mycore.frontend.cli;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

/**
 * Use this class to change log levels of java packages and classes.
 * 
 * @author shermann
 */
@MCRCommandGroup(name="Logging")
public class MCRLoggingCommands extends MCRAbstractCommands {

    private static final Logger LOGGER = Logger.getLogger(MCRLoggingCommands.class);

   /**
     * @param name
     *            the name of the java class or java package to set the log
     *            level for
     * @param logLevelToSet
     *            the log level to set e.g. TRACE, DEBUG, INFO, WARN, ERROR and
     *            FATAL, providing any other value will lead to DEBUG as new log
     *            level
     */
    @MCRCommand(syntax="change log level of {0} to {1}",
    		help="{0} the package or class name for which to change the log level, {1} the log level to set. If the log level cannot be read it is set to DEBUG by default.",
    		order=10)
    synchronized public static void changeLogLevel(String name, String logLevelToSet) {
        LOGGER.info("Setting log level for \"" + name + "\" to \"" + logLevelToSet + "\"");

        Level newLevel = Level.toLevel(logLevelToSet.toUpperCase());
        if (newLevel == null) {
            LOGGER.error("Unknown log level \"" + logLevelToSet + "\"");
            return;
        }

        Logger log = Logger.getLogger(name);
        if (log == null) {
            LOGGER.error("Could not get logger for \"" + name + "\"");
            return;
        }

        LOGGER.info("Change log level from " + log.getLevel() + " to " + newLevel);
        log.setLevel(newLevel);
    }
}
