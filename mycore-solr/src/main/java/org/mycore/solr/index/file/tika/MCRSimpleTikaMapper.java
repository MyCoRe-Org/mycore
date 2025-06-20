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

package org.mycore.solr.index.file.tika;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.config.annotation.MCRProperty;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ValueNode;

/**
 * A simple implementation of the MCRTikaMapper interface. This implementation maps the JSON key to a Solr field name
 * all characters that are not letters or digits are replaced by an underscore and the key is converted to lower case.
 * <p>
 * If the property "StripNamespace" is set to true, the namespace of the key is removed before mapping.
 * If the property "MultiValueField" is set to true, the values are added as multi value fields, otherwise the values
 * will be concatenated to a single string with newlines.
 *
 * @author Sebastian Hofmann
 */
public class MCRSimpleTikaMapper implements MCRTikaMapper {

    private boolean stripNamespace;

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
        TreeNode element,
        SolrInputDocument document,
        Path filePath,
        BasicFileAttributes attributes) throws MCRTikaMappingException {

        String keyWONamespace = isStripNamespace() && key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        String simplifiedKey = MCRTikaMapper.simplifyKeyName(keyWONamespace);

        if (element.isValueNode() && element instanceof ValueNode vn) {
            mapValueNode(document, vn, simplifiedKey);
        } else if (element.isArray() && element instanceof ArrayNode an) {
            mapArrayNode(document, an, simplifiedKey);
        }
    }

    private void mapArrayNode(SolrInputDocument document, ArrayNode an, String simplifiedKey) {
        if (isMultiValueField()) {
            an.forEach(e -> {
                if (e.isValueNode() && e instanceof ValueNode vn) {
                    mapValueNode(document, vn, simplifiedKey);
                }
            });
        } else {
            StringBuilder sb = new StringBuilder();
            an.forEach(e -> {
                if (e.isValueNode() && e instanceof ValueNode vn) {
                    if (!sb.isEmpty()) {
                        sb.append('\n');
                    }
                    sb.append(getValueNodeAsString(vn));
                }
            });
            document.addField(simplifiedKey, sb.toString());
        }
    }

    private void mapValueNode(SolrInputDocument document, ValueNode vn, String simplifiedKey) {
        String value = getValueNodeAsString(vn);
        if (value == null || value.isEmpty()) {
            return;
        }
        document.addField(simplifiedKey, value);
    }

    protected String getValueNodeAsString(ValueNode vn) {
        if (vn.isNull()) {
            return null;
        }

        if (vn.isBoolean()) {
            return String.valueOf(vn.booleanValue());
        }

        if (vn.isIntegralNumber()) {
            return String.valueOf(vn.intValue());
        }

        if (vn.isFloatingPointNumber()) {
            return String.valueOf(vn.doubleValue());
        }

        if (vn.isTextual()) {
            return vn.textValue();
        }

        return null;
    }
}
