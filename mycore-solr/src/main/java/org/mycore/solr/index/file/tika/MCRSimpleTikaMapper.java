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

package org.mycore.solr.index.file.tika;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.config.annotation.MCRProperty;

import com.google.gson.JsonElement;

/**
 * A simple implementation of the MCRTikaMapper interface. This implementation maps the JSON key to a Solr field name
 * all characters that are not letters or digits are replaced by an underscore and the key is converted to lower case.
 *
 * If the property "StripNamespace" is set to true, the namespace of the key is removed before mapping.
 * If the property "MultiValueField" is set to true, the values are added as multi value fields, otherwise the values
 * will be concatenated to a single string with newlines.
 *
 * @author Sebastian Hofmann
 */
public class MCRSimpleTikaMapper implements MCRTikaMapper {

    private boolean stripNamespace = false;

    private boolean multiValue = true;

    @MCRProperty(name = "StripNamespace", required = false)
    public void setStripNamespace(String stripNamespace) {
        this.stripNamespace = Boolean.parseBoolean(stripNamespace);
    }

    public void setStripNamespace(boolean stripNamespace) {
        this.stripNamespace = stripNamespace;
    }

    public boolean isStripNamespace() {
        return stripNamespace;
    }

    @MCRProperty(name = "MultiValueField", required = false)
    public void setMultiValue(String multiValue) {
        this.multiValue = Boolean.parseBoolean(multiValue);
    }

    public void setMultiValue(boolean multiValue) {
        this.multiValue = multiValue;
    }

    public boolean isMultiValueField() {
        return multiValue;
    }

    @Override
    public void map(String key,
        JsonElement element,
        SolrInputDocument document,
        Path filePath,
        BasicFileAttributes attributes) throws MCRTikaMappingException {

        String keyWONamespace = isStripNamespace() && key.contains(":") ? key.substring(key.indexOf(":") + 1) : key;
        String simplifiedKey = MCRTikaMapper.simplifyKeyName(keyWONamespace);

        if (element.isJsonPrimitive()) {
            document.addField(simplifiedKey, element.getAsString());
        } else if (element.isJsonArray()) {
            if (isMultiValueField()) {
                element.getAsJsonArray().forEach(e -> {
                    if (e.isJsonPrimitive()) {
                        document.addField(simplifiedKey, e.getAsString());
                    }
                });
            } else {
                document.addField(simplifiedKey, String.join("\n", element.getAsJsonArray()
                    .asList()
                    .stream()
                    .map(JsonElement::getAsString)
                    .toArray(String[]::new)));
            }
        }
    }
}
