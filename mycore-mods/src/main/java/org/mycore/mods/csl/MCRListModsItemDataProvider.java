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
 *
 *
 */

package org.mycore.mods.csl;

import de.undercouch.citeproc.csl.CSLItemData;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.csl.MCRItemDataProvider;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Does the same as @{@link MCRModsItemDataProvider} but you can provide multiple objects
 */
public class MCRListModsItemDataProvider extends MCRItemDataProvider {

    private HashMap<String , CSLItemData> store = new HashMap<>();

    @Override
    public void addContent(MCRContent content) throws IOException, JDOMException, SAXException {
        Document document = content.asXML();
        List<Element> objects = document.getRootElement().getChildren("mycoreobject");

        for (Element object : objects) {
            Element copy = object.clone().detach();
            String objectID = copy.getAttributeValue("ID");
            MCRModsItemDataProvider midp = new MCRModsItemDataProvider();
            midp.addContent(new MCRJDOMContent(copy));
            CSLItemData cslItemData = midp.retrieveItem(objectID);
            store.put(objectID, cslItemData);
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
    public String[] getIds() {
        return this.store.keySet().toArray(new String[0]);
    }
}
