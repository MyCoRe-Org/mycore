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

package org.mycore.webtools.upload.exception;

import org.mycore.services.i18n.MCRTranslation;

import java.io.Serial;

/**
 * Should be thrown if a parameter required by an upload handler is not valid. E.g. a classification does not exist.
 */
public class MCRInvalidUploadParameterException extends MCRUploadException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String parameterName;

    private final String wrongReason;

    private final String badValue;

    public MCRInvalidUploadParameterException(String parameterName, String badValue, String wrongReason) {
        this(parameterName, badValue, wrongReason, false);
    }

    public MCRInvalidUploadParameterException(String parameterName, String badValue, String wrongReason,
        boolean translateReason) {
        super("component.webtools.upload.invalid.parameter", parameterName, badValue,
            translateReason ? MCRTranslation.translate(wrongReason) : wrongReason);
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
