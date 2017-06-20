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

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRActiveLinkException;

/**
 * Shows details about an exception that occured during command processing
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRCLIExceptionHandler {

    private final static Logger LOGGER = LogManager.getLogger(MCRCLIExceptionHandler.class);

    public static void handleException(InvocationTargetException ex) {
        handleException(ex.getTargetException());
    }

    public static void handleException(ExceptionInInitializerError ex) {
        handleException(ex.getCause());
    }

    public static void handleException(MCRActiveLinkException ex) {
        showActiveLinks(ex);
        showException(ex);
    }

    private static void showActiveLinks(MCRActiveLinkException activeLinks) {
        MCRCommandLineInterface
            .output("There are links active preventing the commit of work, see error message for details.");
        MCRCommandLineInterface.output("The following links where affected:");

        Map<String, Collection<String>> links = activeLinks.getActiveLinks();

        for (String curDest : links.keySet()) {
            LOGGER.debug("Current Destination: " + curDest);
            Collection<String> sources = links.get(curDest);
            for (String source : sources)
                MCRCommandLineInterface.output(source + " ==> " + curDest);
        }
    }

    public static void handleException(Throwable ex) {
        showException(ex);
    }

    private static void showException(Throwable ex) {
        MCRCommandLineInterface.output("");
        MCRCommandLineInterface.output("Exception occured: " + ex.getClass().getName());
        MCRCommandLineInterface.output("Exception message: " + ex.getLocalizedMessage());
        MCRCommandLineInterface.output("");
        showStackTrace(ex);
        showCauseOf(ex);
    }

    private static void showStackTrace(Throwable ex) {
        String trace = MCRException.getStackTraceAsString(ex);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(trace);
        } else {
            MCRCommandLineInterface.output(trace);
        }
    }

    private static void showCauseOf(Throwable ex) {
        ex = ex.getCause();
        if (ex == null)
            return;

        MCRCommandLineInterface.output("");
        MCRCommandLineInterface.output("This exception was caused by:");
        handleException(ex);
    }
}
