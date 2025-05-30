/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.common;

import java.io.Serial;

/**
 * Instances of this class represent a general exception thrown by any part of
 * the MyCoRe implementation classes.
 * 
 * Note that this class extends <code>java.lang.Exception</code>. Any call of
 * a method that throws subclass of this class must be catched and handled in
 * some way.
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @see java.lang.Exception
 */
public class MCRCatchException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Counter to prevent a recursion between getStackTrace() and toString() */
    private int toStringInvocationCounter;

    /**
     * Creates a new MCRCatchException with an error message
     * 
     * @param message
     *            the error message for this exception
     */
    public MCRCatchException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * 
     * Note that the detail message associated with cause is not automatically
     * incorporated in this exception's detail message.
     * 
     * @param message
     *            the detail message
     * @param cause the
     *            cause (A null value is permitted, and indicates that the cause
     *            is nonexistent or unknown.)
     * @see Exception#Exception(java.lang.String, java.lang.Throwable)
     */
    public MCRCatchException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns a String containing the invocation stack trace for this exception
     * 
     * @return a String containing the invocation stack trace for this exception
     * @see MCRException#getStackTraceAsString(Throwable)
     */
    public String getStackTraceAsString() {
        return MCRException.getStackTraceAsString(this);
    }

    /**
     * Returns a String representation of this exception and all its properties
     * 
     * @return a String representation of this exception and all its properties
     */
    @Override
    public synchronized String toString() {
        // Use counter to prevent a recursion between getStackTrace() and
        // toString()
        toStringInvocationCounter++;

        if (toStringInvocationCounter > 1) {
            return super.toString();
        }

        StringBuilder sb = new StringBuilder();

        sb.append("MyCoRe Exception: ").append(getClass().getName());
        String ls = System.lineSeparator();
        sb.append(ls).append(ls);
        sb.append("Message:").append(ls);
        sb.append(getMessage());
        sb.append(ls).append(ls);
        sb.append("Stack trace:").append(ls);
        sb.append(getStackTraceAsString());

        if (getCause() != null) {
            sb.append(ls);
            sb.append("This exception was thrown because of the following underlying exception:");
            sb.append(ls).append(ls);
            sb.append(getCause().getClass().getName());
            sb.append(ls).append(ls);

            String msg = getCause().getLocalizedMessage();

            if (msg != null) {
                sb.append("Message:").append(ls).append(msg);
                sb.append(ls).append(ls);
            }

            sb.append("Stack trace:").append(ls);
            sb.append(MCRException.getStackTraceAsString(getCause()));
        }

        toStringInvocationCounter--;

        return sb.toString();
    }
}
