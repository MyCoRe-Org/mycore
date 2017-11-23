/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import javax.xml.transform.TransformerException;

import org.apache.xml.utils.WrappedRuntimeException;

/**
 * Tries to find the cause of an exception by diving down
 * those exceptions that wrap other exceptions, recursively.
 * The exception at the bottom of the stack is returned.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRExceptionCauseFinder {

    public static Exception getCause(TransformerException ex) {
        Throwable cause = ex.getException();
        return (cause == null ? ex : getCause(cause));
    }

    public static Exception getCause(Throwable ex) {
        Throwable cause = ex.getCause();
        return (cause == null ? (Exception) ex : getCause(cause));
    }

    public static Exception getCause(WrappedRuntimeException ex) {
        return getCause(ex.getException());
    }
}
