/*
 * $Id$ 
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
