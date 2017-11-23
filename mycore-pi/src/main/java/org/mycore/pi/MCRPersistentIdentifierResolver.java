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

package org.mycore.pi;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;

public abstract class MCRPersistentIdentifierResolver<T extends MCRPersistentIdentifier> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String name;

    public MCRPersistentIdentifierResolver(String name) {
        this.name = name;
    }

    public abstract Stream<String> resolve(T identifier) throws MCRIdentifierUnresolvableException;

    public Stream<String> resolveSuppress(T identifier) {
        try {
            return resolve(identifier);
        } catch (MCRIdentifierUnresolvableException e) {
            LOGGER.info(e);
            return Stream.empty();
        }
    }

    public String getName() {
        return name;
    }
}
