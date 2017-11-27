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

package org.mycore.pi.frontend.model;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRPIErrorJSON {

    public String message;

    public String stackTrace;

    public String translatedAdditionalInformation;

    public String code;

    public MCRPIErrorJSON(String message) {
        this(message, null);
    }

    public MCRPIErrorJSON(String message, Exception e) {
        this.message = message;

        if (e instanceof MCRPersistentIdentifierException) {
            MCRPersistentIdentifierException identifierException = (MCRPersistentIdentifierException) e;
            identifierException.getCode().ifPresent(code -> this.code = Integer.toHexString(code));
            identifierException.getTranslatedAdditionalInformation()
                .ifPresent(msg -> this.translatedAdditionalInformation = msg);
        }

        if (e != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            stackTrace = sw.toString();
        }
    }
}
