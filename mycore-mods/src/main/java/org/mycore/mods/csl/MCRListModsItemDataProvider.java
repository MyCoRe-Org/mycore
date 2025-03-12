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

package org.mycore.mods.csl;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedMap;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.MCRCache;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.csl.MCRItemDataProvider;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRXMLConstants;
import org.xml.sax.SAXException;

import de.undercouch.citeproc.csl.CSLItemData;

/**
 * Does the same as @{@link MCRModsItemDataProvider} but you can provide multiple objects
 */
public class MCRListModsItemDataProvider extends MCRItemDataProvider {

    protected static MCRCache<String, CSLItemData> cslCache = new MCRCache<>(2000, "CSL Mods Data");

    private final SequencedMap<String, CSLItemData> store = new LinkedHashMap<>();

    @Override
    public void addContent(MCRContent content) throws IOException, JDOMException, SAXException {
        Document document = content.asXML();
        List<Element> objects = document.getRootElement().getChildren(MCRObject.ROOT_NAME);

        for (Element object : objects) {
            Element copy = object.clone().detach();
            String objectID = copy.getAttributeValue(MCRXMLConstants.ID);
            MCRObjectID mcrObjectID = MCRObjectID.getInstance(objectID);
            CSLItemData itemData = cslCache.getIfUpToDate(objectID, MCRXMLMetadataManager.getInstance()
                .getLastModified(mcrObjectID));
            if (itemData == null) {
                MCRModsItemDataProvider midp = new MCRModsItemDataProvider();
                midp.addContent(new MCRJDOMContent(copy));
                itemData = midp.retrieveItem(objectID);
                cslCache.put(objectID, itemData);
            }

            store.put(objectID, itemData);
        }
    }

    @Override
    public void reset() {
        this.store.clear();
    }

    @Override
    public CSLItemData retrieveItem(String s) {
        return this.store.get(s);
    }

    @Override
    public Collection<String> getIds() {
        return new LinkedHashSet<>(this.store.keySet());
    }
}
