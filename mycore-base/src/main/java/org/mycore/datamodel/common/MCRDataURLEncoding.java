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
package org.mycore.datamodel.common;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public enum MCRDataURLEncoding {

    /**
     * Base64 encoding
     */
    BASE64("base64"),

    /**
     * URL encoding
     */
    URL("");

    private final String value;

    MCRDataURLEncoding(final String value) {
        this.value = value;
    }

    /**
     * Returns the data url encoding.
     * 
     * @return the set data url encoding
     */
    public String value() {
        return value;
    }

    /**
     * Returns the data url encoding from given value.
     * 
     * @param value the data url encoding
     * @return the the data url encoding for value
     */
    public static MCRDataURLEncoding fromValue(final String value) {
        for (MCRDataURLEncoding encodings : MCRDataURLEncoding.values()) {
            if (encodings.value.equals(value)) {
                return encodings;
            }
        }
        throw new IllegalArgumentException(value);
    }
}
