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

/**
 * Instances of this class represent a general exception thrown by any part of
 * the MyCoRe implementation classes.
 * 
 * Note that this class extends <code>java.lang.Exception</code>. Any call of
 * a method that throws subclass of this class must be catched and handled in
 * some way.
 * 
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 * 
 * @see java.lang.Exception
 */
public class MCRCatchException extends Exception {
    private static final long serialVersionUID = -2244850451757768863L;

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

        if (getCause() != null) {
            sb.append("\n");
            sb.append("This exception was thrown because of the following underlying exception:\n\n");
            sb.append(getCause().getClass().getName());
            sb.append("\n\n");

            String msg = getCause().getLocalizedMessage();

            if (msg != null) {
                sb.append("Message:\n").append(msg).append("\n\n");
            }

            sb.append("Stack trace:\n");
            sb.append(MCRException.getStackTraceAsString(getCause()));
        }

        toStringInvocationCounter--;

        return sb.toString();
    }
}