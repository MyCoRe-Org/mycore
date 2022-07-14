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

package org.mycore.csl;

import java.io.IOException;
import java.util.stream.Collectors;

import org.jdom2.JDOMException;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStringContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.xml.sax.SAXException;

import de.undercouch.citeproc.helper.json.JsonBuilder;
import de.undercouch.citeproc.helper.json.StringJsonBuilderFactory;

public class MCRCSLJSONTransformer extends MCRContentTransformer {
    private String configuredItemProviderProperty;

    @Override
    public void init(String id) {
        super.init(id);
        configuredItemProviderProperty = "MCR.ContentTransformer." + id + "." + MCRCSLTransformer.ITEM_PROVIDER;
    }

    private MCRItemDataProvider createItemDataProvider() {
        return MCRConfiguration2.<MCRItemDataProvider>getInstanceOf(configuredItemProviderProperty)
            .orElseThrow(() -> MCRConfiguration2.createConfigurationException(configuredItemProviderProperty));
    }

    @Override
    public String getMimeType() {
        return "application/json";
    }

    @Override
    public String getEncoding() {
        return MCRConstants.DEFAULT_ENCODING;
    }

    @Override
    public String getFileExtension() {
        return "json";
    }

    @Override
    public MCRContent transform(MCRContent source) throws IOException {
        final JsonBuilder jsonBuilder = new StringJsonBuilderFactory().createJsonBuilder();
        MCRItemDataProvider dataProvider = createItemDataProvider();
        try {
            dataProvider.addContent(source);
        } catch (JDOMException | SAXException e) {
            throw new IOException(e);
        }
        final String jsonArray = dataProvider.getIds().stream()
            .map(dataProvider::retrieveItem)
            .map(jsonBuilder::toJson)
            .map(Object::toString)
            .collect(Collectors.joining(",", "[", "]"));
        return new MCRStringContent(jsonArray);
    }
}
