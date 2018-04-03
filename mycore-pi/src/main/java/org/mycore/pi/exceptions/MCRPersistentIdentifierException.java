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

package org.mycore.pi.exceptions;

import java.util.Optional;

import org.mycore.common.MCRCatchException;

public class MCRPersistentIdentifierException extends MCRCatchException {

    private final String translatedAdditionalInformation;

    private final Integer code;

    public MCRPersistentIdentifierException(String message) {
        super(message);
        translatedAdditionalInformation = null;
        code = null;
    }

    public MCRPersistentIdentifierException(String message, Throwable cause) {
        super(message, cause);
        translatedAdditionalInformation = null;
        code = null;
    }

    public MCRPersistentIdentifierException(String message, String translatedAdditionalInformation, int code) {
        this(message, translatedAdditionalInformation, code, null);
    }

    public MCRPersistentIdentifierException(String message, String translatedAdditionalInformation, int code,
        Exception cause) {
        super(message, cause);

        this.translatedAdditionalInformation = translatedAdditionalInformation;
        this.code = code;
    }

    public Optional<String> getTranslatedAdditionalInformation() {
        return Optional.ofNullable(translatedAdditionalInformation);
    }

    public Optional<Integer> getCode() {
        return Optional.ofNullable(code);
    }
}
