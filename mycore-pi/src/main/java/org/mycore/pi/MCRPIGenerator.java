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

import static org.mycore.pi.MCRPIService.GENERATOR_CONFIG_PREFIX;

import java.util.Map;

import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public abstract class MCRPIGenerator<T extends MCRPersistentIdentifier> {

    private String generatorID;
    private Map<String, String> properties;

    public final Map<String, String> getProperties() {
        return properties;
    }

    @MCRPostConstruction
    public void init(String property){
        generatorID = property.substring(GENERATOR_CONFIG_PREFIX.length());
    }

    @MCRProperty(name = "*")
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
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

    /**
     * checks if the property exists and throws a exception if not.
     * @param propertyName to check
     * @throws MCRConfigurationException if property does not exist
     */
    protected void checkPropertyExists(final String propertyName) throws MCRConfigurationException {
        if (!getProperties().containsKey(propertyName)) {
            throw new MCRConfigurationException(
                "Missing property " + GENERATOR_CONFIG_PREFIX + getGeneratorID() + "." + propertyName);
        }
    }

    public String getGeneratorID() {
        return generatorID;
    }
}
