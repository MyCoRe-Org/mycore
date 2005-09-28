/*
 * $RCSfile$
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

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
    /** the embedded exception that was thrown by an underlying system */
    protected Exception exception;

    /**
     * Creates a new MCRException with an error message
     * 
     * @param message
     *            the error message for this exception
     */
    public MCRException(String message) {
        super(message);
    }

    /**
     * Creates a new MCRException with an error message and a reference to an
     * exception thrown by an underlying system. Normally, this exception will
     * be the cause why we would throw an MCRException, e. g. when something in
     * the datastore goes wrong.
     * 
     * @param message
     *            the error message for this exception
     * @param exception
     *            the exception that was thrown by an underlying system
     */
    public MCRException(String message, Exception exception) {
        this(message);
        this.exception = exception;
    }

    /**
     * Returns the exception thrown by an underlying system
     * 
     * @return the exception thrown by an underlying system
     */
    public Exception getException() {
        return exception;
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PrintStream buffer = new PrintStream(baos);
            ex.printStackTrace(buffer);
            buffer.close();
        } catch (Exception willNeverBeThrown) {
        }

        return baos.toString();
    }

    /** Counter to prevent a recursion between getStackTrace() and toString() */
    private int toStringInvocationCounter = 0;

    /**
     * Returns a String representation of this exception and all its properties
     * 
     * @return a String representation of this exception and all its properties
     */
    public synchronized String toString() {
        // Use counter to prevent a recursion between getStackTrace() and
        // toString()
        toStringInvocationCounter++;

        if (toStringInvocationCounter > 1) {
            return super.toString();
        }

        StringBuffer sb = new StringBuffer();

        sb.append("MyCoRe Exception: ").append(getClass().getName());
        sb.append("\n\n");
        sb.append("Message:\n");
        sb.append(getMessage()).append("\n\n");
        sb.append("Stack trace:\n");
        sb.append(getStackTraceAsString());

        if (exception != null) {
            sb.append("\n");
            sb.append("This exception was thrown because of the following underlying exception:\n\n");
            sb.append(exception.getClass().getName());
            sb.append("\n\n");

            String msg = exception.getLocalizedMessage();

            if (msg != null) {
                sb.append("Message:\n").append(msg).append("\n\n");
            }

            sb.append("Stack trace:\n");
            sb.append(getStackTraceAsString(exception));
        }

        toStringInvocationCounter--;

        return sb.toString();
    }
}
