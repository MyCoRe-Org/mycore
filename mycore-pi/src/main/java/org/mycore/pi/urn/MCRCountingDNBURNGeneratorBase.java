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

package org.mycore.pi.urn;

import java.util.Objects;
import java.util.regex.Pattern;

import org.mycore.pi.util.MCRPIGeneratorUtils;

/**
 * A Generator which helps to generate a URN with a counter inside.
 */
public abstract class MCRCountingDNBURNGeneratorBase extends MCRDNBURNGeneratorBase {

    private final MCRPIGeneratorUtils.Counter counter;

    MCRCountingDNBURNGeneratorBase(MCRPIGeneratorUtils.Counter counter, String namespace, String delimiter) {
        super(namespace, delimiter);
        this.counter = Objects.requireNonNull(counter, "Counter must not be null");
    }

    /**
     * @deprecated Use {@link #MCRCountingDNBURNGeneratorBase(MCRPIGeneratorUtils.Counter, String, String)} instead.
     */
    @Deprecated(forRemoval = true)
    MCRCountingDNBURNGeneratorBase(String namespace) {
        this(MCRPIGeneratorUtils.SHARED_COUNTER, namespace, "");
    }

    /**
     * Gets the count for a specific pattern and increase the internal counter. If there is no internal counter it will
     * look into the Database and detect the highest count with the pattern.
     * @param pattern a regex pattern which can be used to extract count value in existing identifiers.
     *                The first capturing group captures the count.
     *                Example: <code>[0-9]+-mods-2017-([0-9][0-9][0-9][0-9])-[0-9]</code>
     *                will match <code>31-mods-2017-0003-3</code> and the returned count should be <code>4</code>
     *                (<code>3+1</code>). All non-variable parts of the pattern should be protected with
     *                {@link Pattern#quote(String)}, in order to make patterns comparable.
     * @return the next count
     */
    public final synchronized int getCount(String pattern) {
        return counter.getCount(MCRDNBURN.TYPE, pattern);
    }

}
