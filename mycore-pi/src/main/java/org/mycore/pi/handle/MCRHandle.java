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

package org.mycore.pi.handle;

import java.util.Objects;

import org.mycore.pi.MCRPersistentIdentifier;

public class MCRHandle implements MCRPersistentIdentifier {

    private final String prefix;

    private final String suffix;

    MCRHandle(String prefix, String suffix) {
        Objects.requireNonNull(prefix);
        Objects.requireNonNull(suffix);

        if (prefix.isEmpty() || suffix.isEmpty()) {
            throw new IllegalArgumentException("prefix and suffix need to be not empty: " + prefix + "/" + suffix);
        }

        this.prefix = prefix;
        this.suffix = suffix;
    }

    MCRHandle(String str) {
        this(str.split("/", 2)[0], str.split("/", 2)[1]);
    }

    @Override
    public String toString() {
        return prefix + "/" + suffix;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    @Override
    public String asString() {
        return toString();
    }
}
