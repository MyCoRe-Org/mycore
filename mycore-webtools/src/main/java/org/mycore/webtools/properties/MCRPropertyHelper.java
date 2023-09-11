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

package org.mycore.webtools.properties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.mycore.common.config.MCRConfigurationInputStream;
import org.mycore.common.config.MCRProperties;

class MCRPropertyHelper {

    private final LinkedHashMap<String, byte[]> configFileContents;

    MCRPropertyHelper() throws IOException {
        configFileContents = MCRConfigurationInputStream.getConfigFileContents("mycore.properties");
    }

    public Map<String, List<MCRProperty>> analyzeProperties() throws IOException {
        Properties properties = new MCRProperties();
        Properties currentProperties = new MCRProperties();
        final AtomicReference<Properties> oldProperties = new AtomicReference<Properties>(null);

        LinkedHashMap<String, List<MCRProperty>> analyzedProperties = new LinkedHashMap<>();

        for (Map.Entry<String, byte[]> componentContentEntry : configFileContents.entrySet()) {
            String component = componentContentEntry.getKey();
            byte[] value = componentContentEntry.getValue();
            List<MCRProperty> componentList = analyzedProperties.computeIfAbsent(component, k1 -> new LinkedList<>());

            try(ByteArrayInputStream bais = new ByteArrayInputStream(value)){
                properties.load(bais);
                bais.reset();
                currentProperties.load(bais);
            }

            currentProperties.forEach((k, v) -> {
                String propertyName = (String) k;
                String propertyValue = properties.getProperty(propertyName);
                String oldValue = Optional.ofNullable(oldProperties.get()).map(op -> op.getProperty(propertyName))
                        .orElse(null);

                componentList.add(new MCRProperty(component, propertyName, oldValue, propertyValue));
                currentProperties.put(propertyName, propertyValue);
            });

            currentProperties.clear();
            oldProperties.set((Properties) properties.clone());
        }
        return analyzedProperties;
    }

}
