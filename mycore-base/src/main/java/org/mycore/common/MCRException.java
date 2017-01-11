/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.logging.log4j.LogManager;

/**
 * Instances of this class represent a general exception thrown by any part of
 * the MyCoRe implementation classes.
 * 
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 * 
 * @see RuntimeException
 */
public class MCRException extends RuntimeException {
    private static final long serialVersionUID = -3396055962010289244L;

    /**
     * Creates a new MCRException with an error message
     * 
     * @param message
     *            the error message for this exception
     */
    public MCRException(String message) {
        super(message);
    }

    public MCRException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new MCRException with an error message and a reference to an
     * exception thrown by an underlying system. Normally, this exception will
     * be the cause why we would throw an MCRException, e. g. when something in
     * the datastore goes wrong.
     * 
     * @param message
     *            the error message for this exception
     * @param cause
     *            the cause (which is saved for later retrieval by the {@link Throwable#getCause()} method). (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public MCRException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns a String containing the invocation stack trace for this exception
     * 
     * @return a String containing the invocation stack trace for this exception
     */
    public String getStackTraceAsString() {
        return getStackTraceAsString(this);
    }

    /**
     * Returns a String containing the invocation stack trace of an exception
     * 
     * @param ex
     *            the exception you want the stack trace of
     * @return the invocation stack trace of an exception
     */
    public static String getStackTraceAsString(Throwable ex) {
        // We let Java print the stack trace to a buffer in memory to be able to
        // get it as String:
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            ex.printStackTrace(pw);
            pw.flush();
            return sw.toString();
        } catch (IOException e) {
            LogManager.getLogger(MCRException.class).warn("Error while transforming stack trace to String.", e);
            return null;
        }

    }

}
