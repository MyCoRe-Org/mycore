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

package org.mycore.common.xsl.uriresolver;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.access.MCRAccessManager;

public class MCRACLResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ACTION_PARAM = "action";

    private static final String OBJECT_ID_PARAM = "object";

    /**
     * Returns access controll rules as XML
     */
    @Override
    public Source resolve(String href, String base) {
        String key = href.substring(href.indexOf(':') + 1);
        LOGGER.debug("Reading xml from query result using key :{}", key);

        String[] param;
        StringTokenizer tok = new StringTokenizer(key, "&");
        Map<String, String> params = new HashMap<>();

        while (tok.hasMoreTokens()) {
            param = tok.nextToken().split("=");
            params.put(param[0], param[1]);
        }

        String action = params.get(ACTION_PARAM);
        String objId = params.get(OBJECT_ID_PARAM);

        if (action == null || objId == null) {
            return null;
        }

        Element container = new Element("servacls").setAttribute("class", "MCRMetaAccessRule");
        if (MCRAccessManager.implementsRulesInterface()) {
            if (action.equals("all")) {
                for (String permission : MCRAccessManager.getPermissionsForID(objId)) {
                    // one pool Element under access per defined AccessRule in pool for (Object-)ID
                    addRule(container, permission,
                        MCRAccessManager.requireRulesInterface().getRule(objId, permission));
                }
            } else {
                addRule(container, action, MCRAccessManager.requireRulesInterface().getRule(objId, action));
            }
        }
        return new JDOMSource(container);
    }

    private void addRule(Element root, String pool, Element rule) {
        if (rule != null && pool != null) {
            Element poolElement = new Element("servacl").setAttribute("permission", pool);
            poolElement.addContent(rule);
            root.addContent(poolElement);
        }
    }

}
