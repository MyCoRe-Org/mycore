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

import java.util.HashMap;
import java.util.Map;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

import static org.mycore.pi.MCRPIService.GENERATOR_CONFIG_PREFIX;

public abstract class MCRPIGenerator<T extends MCRPersistentIdentifier> {

    private String generatorID;

    public MCRPIGenerator(String generatorID) {
        this.generatorID = generatorID;
    }

    protected final Map<String, String> getProperties() {
        Map<String, String> propertiesMap = MCRConfiguration.instance().getPropertiesMap(
            GENERATOR_CONFIG_PREFIX + generatorID + ".");

        Map<String, String> shortened = new HashMap<>();

        propertiesMap.keySet().forEach(key -> {
            String newKey = key.substring(GENERATOR_CONFIG_PREFIX.length() + generatorID.length() + 1);
            shortened.put(newKey, propertiesMap.get(key));
        });

        return shortened;
    }

    /**
     * generates a {@link MCRPersistentIdentifier}
     *
     * @param mcrBase    the mycore object for which the identifier is generated
     * @param additional additional information dedicated to the object like a mcrpath
     * @return a unique persistence identifier
     * @throws MCRPersistentIdentifierException if something goes wrong while generating
     */
    public abstract T generate(MCRBase mcrBase, String additional) throws MCRPersistentIdentifierException;

}
