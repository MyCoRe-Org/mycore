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

package org.mycore.common.xml;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Handles errors during XML parsing.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXMLParserErrorHandler implements ErrorHandler {

    private final static Logger LOGGER = LogManager.getLogger(MCRXMLParserErrorHandler.class);

    protected boolean silent;

    public MCRXMLParserErrorHandler() {
        this(false);
    }

    public MCRXMLParserErrorHandler(boolean silent) {
        this.silent = silent;
    }

    /**
     * Handles parser warnings
     */
    public void warning(SAXParseException ex) {
        if (!silent) {
            LOGGER.warn(getSAXErrorMessage(ex), ex);
        }
    }

    /**
     * Handles parse errors
     */
    public void error(SAXParseException ex) {
        if (!silent) {
            LOGGER.error(getSAXErrorMessage(ex), ex);
        }
        throw new RuntimeException(ex);
    }

    /**
     * Handles fatal parse errors
     */
    public void fatalError(SAXParseException ex) {
        if (!silent) {
            LOGGER.fatal(getSAXErrorMessage(ex));
        }
        throw new RuntimeException(ex);
    }

    /**
     * Returns a text indicating at which line and column the error occured.
     * 
     * @param ex the SAXParseException exception
     * @return the location string
     */
    public static String getSAXErrorMessage(SAXParseException ex) {
        StringBuilder str = new StringBuilder();

        String systemId = ex.getSystemId();
        if (systemId != null) {
            int index = systemId.lastIndexOf('/');

            if (index != -1) {
                systemId = systemId.substring(index + 1);
            }

            str.append(systemId).append(": ");
        }

        str.append("line ").append(ex.getLineNumber());
        str.append(", column ").append(ex.getColumnNumber());
        str.append(", ");
        str.append(ex.getLocalizedMessage());

        return str.toString();
    }
}
