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

package org.mycore.frontend.xeditor.includes;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

/** 
 * Map to lookup preloaded and imported XML elements by their @id.
 * 
 * @author Frank L\U00FCtzenkirchen
 */
public class MCRElementLookupMap {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ATTR_ID = "id";

    private Map<String, Element> lookupMap = new HashMap<>();

    /**
     * Returns the element with the given @id, if any.
     * 
     * @param id the id attribute
     * @return the element, or null if no match found
     */
    Element get(String id) {
        return lookupMap.get(id);
    }

    /**
     * Stores the element for later reference, but
     * only if it has an @id attribute to be used as key.
     *  
     * @param element the xml to be cached
     */
    void offer(Element element) {
        String id = element.getAttributeValue(ATTR_ID);

        if (!StringUtils.isEmpty(id)) {
            put(id, element);
        }
    }

    /**
     * Stores the element for later reference, using the given id.
     *  
     * @param id the id to use as key
     * @param element the xml to be stored
     */
    void put(String id, Element element) {
        LOGGER.debug(() -> "caching element with id " + id);
        lookupMap.put(id, element);
    }
}
