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

package org.mycore.datamodel.ifs;

import java.util.HashMap;
import java.util.List;

import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.xml.MCRURIResolver;

/**
 * Decides which MCRContentStore implementation should be used to store the
 * content of a given file, based on the content type of the file.
 * 
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRSimpleContentStoreSelector implements MCRContentStoreSelector {
    /** the default content store to use if no other rule matches */
    protected String defaultID;

    /**
     * store lookup table where keys are file content type IDs, values are
     * content store IDs
     */
    protected HashMap<String, String> typeStoreMap;

    /** list of all storeIDs * */
    protected String[] storeIDs;

    public MCRSimpleContentStoreSelector() {
        MCRConfiguration config = MCRConfiguration.instance();
        String file = config.getString("MCR.IFS.ContentStoreSelector.ConfigFile");
        Element xml = MCRURIResolver.instance().resolve("resource:" + file);
        if (xml == null) {
            throw new MCRConfigurationException("Could not load configuration file from resource:" + file);
        }

        typeStoreMap = new HashMap<>();

        List<Element> stores = xml.getChildren("store");
        storeIDs = new String[stores.size() + 1];

        for (int i = 0; i < stores.size(); i++) {
            Element store = stores.get(i);
            String storeID = store.getAttributeValue("ID");
            storeIDs[i] = storeID;

            List<Element> types = store.getChildren();

            for (Object type1 : types) {
                Element type = (Element) type1;
                String typeID = type.getTextTrim();

                typeStoreMap.put(typeID, storeID);
            }
        }

        defaultID = xml.getAttributeValue("default");

        // NOTE: if defaultID is listed as a <store> it's inserted twice here
        storeIDs[storeIDs.length - 1] = defaultID;
    }

    public String selectStore(MCRFile file) throws MCRException {
        return getStore(file.getContentTypeID());
    }

    public String selectStore(MCRFileContentType type) {
        return getStore(type.getID());
    }

    private String getStore(String typeID) {
        if (typeStoreMap.containsKey(typeID)) {
            return typeStoreMap.get(typeID);
        }
        return defaultID;
    }

    public String[] getAvailableStoreIDs() {
        return storeIDs;
    }

    @Override
    public String getDefaultStore() {
        return defaultID;
    }
}
