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

package org.mycore.pi.condition;

import java.util.Map;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

public abstract class MCRPIPredicateBase implements MCRPIPredicate {

    private final String propertyPrefix;

    public MCRPIPredicateBase(String propertyPrefix) {
        this.propertyPrefix = propertyPrefix;
    }

    public String getPropertyPrefix() {
        return propertyPrefix;
    }

    @Override
    public Map<String, String> getProperties() {
        return MCRConfiguration2.getSubPropertiesMap(propertyPrefix);
    }

    protected String requireProperty(String key) {
        final Map<String, String> properties = getProperties();
        if (!properties.containsKey(key)) {
            throw new MCRConfigurationException(getPropertyPrefix() + key + " ist not defined!");
        }
        return properties.get(key);
    }

}
