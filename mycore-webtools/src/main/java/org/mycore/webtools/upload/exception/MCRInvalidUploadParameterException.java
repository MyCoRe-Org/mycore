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

package org.mycore.webtools.upload.exception;

/**
 * Should be thrown if a parameter required by an upload handler is not valid. E.g. a classification does not exist.
 */
public class MCRInvalidUploadParameterException extends MCRUploadException {

    private static final long serialVersionUID = 1L;

    private final String parameterName;

    private final String wrongReason;

    private final String badValue;

    public MCRInvalidUploadParameterException(String parameterName, String badValue, String wrongReason) {
        super("component.webtools.upload.invalid.parameter", parameterName, badValue, wrongReason);
        this.parameterName = parameterName;
        this.wrongReason = wrongReason;
        this.badValue = badValue;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getWrongReason() {
        return wrongReason;
    }

    public String getBadValue() {
        return badValue;
    }
}
