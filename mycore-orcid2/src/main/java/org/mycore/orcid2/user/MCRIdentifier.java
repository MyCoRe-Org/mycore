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

package org.mycore.orcid2.user;

import java.util.Locale;
import java.util.Objects;

/**
  Class to store ids.
*/
public class MCRIdentifier {

    private final String type;

    private final String value;

    /**
     * Constructs new MCRIdentifier object with type and value.
     * 
     * @param type the id type
     * @param value the id value
     */
    public MCRIdentifier(String type, String value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Returns the id type.
     *
     * @return id type
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the id value.
     *
     * @return id value
     */
    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MCRIdentifier identifier = (MCRIdentifier) obj;
        return Objects.equals(type, identifier.type) && Objects.equals(value, identifier.value);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s:%s", type, value);
    }
}
