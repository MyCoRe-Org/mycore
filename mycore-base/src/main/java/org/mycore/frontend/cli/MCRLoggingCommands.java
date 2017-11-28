/**
 *
 */
package org.mycore.frontend.cli;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

/**
 * Use this class to change log levels of java packages and classes.
 *
 * @author shermann
 */
@MCRCommandGroup(name = "Logging Commands")
public class MCRLoggingCommands extends MCRAbstractCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * @param name
     *            the name of the java class or java package to set the log
     *            level for
     * @param logLevelToSet
     *            the log level to set e.g. TRACE, DEBUG, INFO, WARN, ERROR and
     *            FATAL, providing any other value will lead to DEBUG as new log
     *            level
     */
    @MCRCommand(syntax = "change log level of {0} to {1}",
        help = "{0} the package or class name for which to change the log level, {1} the log level to set.",
        order = 10)
    public static synchronized void changeLogLevel(String name, String logLevelToSet) {
        LOGGER.info("Setting log level for \"{}\" to \"{}\"", name, logLevelToSet);
        Level newLevel = Level.getLevel(logLevelToSet);
        if (newLevel == null) {
            LOGGER.error("Unknown log level \"" + logLevelToSet + "\"");
            return;
        }
        Logger log = "ROOT".equals(name) ? LogManager.getRootLogger() : LogManager.getLogger(name);
        if (log == null) {
            LOGGER.error("Could not get logger for \"" + name + "\"");
            return;
        }
        LOGGER.info("Change log level from {} to {}", log.getLevel(), newLevel);
        Configurator.setLevel(log.getName(), newLevel);
    }
}
