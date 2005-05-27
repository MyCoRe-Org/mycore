/**
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
 *
 **/

package org.mycore.common;

/**
 * This class provides some static utility methods for checking values of
 * arguments and conditions. This can be used to ensure that a method is not
 * invoked with null arguments, for example.
 * 
 * Here is a sample usage, where an MCRUsageException is thrown when "name" is
 * empty:
 * 
 * <pre>
 * public void sayHelloTo(String name) {
 *     MCRArgumentChecker.ensureNotEmpty(name, &quot;name&quot;);
 *     System.out.println(&quot;Hello &quot; + name);
 * }
 * </pre>
 */
public abstract class MCRArgumentChecker {
    /**
     * Ensures that a given boolean expression is true, or throws an
     * MCRUsageException with the error message provided otherwise.
     * 
     * @param expression
     *            the boolean expression that should be true
     * @param message
     *            the error message to be thrown when the expression is not true
     * @throws MCRUsageException
     *             if the expression is not true
     */
    public static void ensureIsTrue(boolean expression, String message) {
        if (!expression)
            throw new MCRUsageException(message);
    }

    /**
     * Ensures that a given boolean expression is false, or throws an
     * MCRUsageException with the error message provided otherwise.
     * 
     * @param expression
     *            the boolean expression that should be false
     * @param message
     *            the error message to be thrown when the expression is true
     * @throws MCRUsageException
     *             if the expression is true
     */
    public static void ensureIsFalse(boolean expression, String message) {
        ensureIsTrue(!expression, message);
    }

    /**
     * Ensures that a given variable does not contain a null reference, or
     * throws an MCRUsageException with an error message otherwise.
     * 
     * @param o
     *            the Object variable that must not contain a null reference
     * @param argumentName
     *            the name of the argument variable that must not be null
     * @throws MCRUsageException
     *             if o is null
     */
    public static void ensureNotNull(Object o, String argumentName) {
        ensureIsTrue(o != null, argumentName + " is null");
    }

    /**
     * Ensures that a given String variable does not contain a null reference,
     * or a String of length 0, or a String containing only blanks.
     * 
     * @param s
     *            the String variable that must not be null of empty
     * @param argumentName
     *            the name of the String argument
     * @throws MCRUsageException
     *             if s is null or s.trim().length() is 0
     */
    public static void ensureNotEmpty(String s, String argumentName) {
        ensureNotNull(s, argumentName);
        ensureIsTrue(s.trim().length() > 0, argumentName
                + " is an empty String");
    }

    /**
     * Ensures that a given numeric value is not negative.
     * 
     * @param value
     *            the value that must not be negative
     * @param argumentName
     *            the name of the number argument
     * @throws MCRUsageException
     *             if value is negative
     */
    public static void ensureNotNegative(double value, String argumentName) {
        ensureIsTrue(value >= 0, argumentName + " is negative");
    }
}

