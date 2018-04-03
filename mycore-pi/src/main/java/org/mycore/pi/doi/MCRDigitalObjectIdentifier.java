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

package org.mycore.pi.doi;

import java.util.Locale;

import org.mycore.pi.MCRPersistentIdentifier;

public class MCRDigitalObjectIdentifier implements MCRPersistentIdentifier {

    public static final String TYPE = "doi";

    public static final String TEST_DOI_PREFIX = "10.5072";

    private String prefix;

    private String suffix;

    protected MCRDigitalObjectIdentifier(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public MCRDigitalObjectIdentifier toTestPrefix() {
        return new MCRDigitalObjectIdentifier(TEST_DOI_PREFIX, suffix);
    }

    @Override
    public String asString() {
        return String.format(Locale.ENGLISH, "%s/%s", prefix, suffix);
    }
}
