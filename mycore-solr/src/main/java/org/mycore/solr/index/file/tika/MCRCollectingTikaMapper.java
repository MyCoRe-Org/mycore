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

import org.apache.solr.common.SolrInputDocument;

import com.fasterxml.jackson.databind.node.ValueNode;

/**
 * A simple implementation of the MCRTikaMapper interface. 
 * This implementation converts JSON key and value of a Tika metadata field 
 * into a property-like ('key=value') string and stores it into the Solr field `tika_metadata`;
 * If the property "StripNamespace" is set to true, the namespace of the key is removed before mapping.
 * If the property "MultiValueField" is set to true, the values are added as multi value fields, otherwise the values
 * will be concatenated to a single string with newlines.
 *
 * @author Robert Stephan
 */
public class MCRCollectingTikaMapper extends MCRSimpleTikaMapper implements MCRTikaMapper {

    private static final String SOLR_FIELD_4_TIKA_METADATA = "tika_metadata";

    @Override
    protected void mapValueNode(SolrInputDocument document, ValueNode vn, String simplifiedKey) {
        String value = getValueNodeAsString(vn);
        if (value == null || value.isEmpty()) {
            return;
        }
        document.addField(SOLR_FIELD_4_TIKA_METADATA, simplifiedKey + "=" + value);
    }
}
