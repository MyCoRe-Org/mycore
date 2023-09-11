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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.frontend.servlets.MCRContentServlet;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MCRPropertyHelperContentServlet extends MCRContentServlet {

    @Override
    public MCRContent getContent(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {

        if (!MCRSessionMgr.getCurrentSession().getUserInformation().getUserID()
            .equals(MCRSystemUserInformation.getSuperUserInstance().getUserID())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        MCRPropertyHelper propertyHelper = new MCRPropertyHelper();
        Map<String, List<MCRProperty>> map = propertyHelper.analyzeProperties();

        Element propertiesElement = new Element("properties-analyze");
        for (String component : map.keySet()) {
            Element componentElement = new Element("component");
            componentElement.setAttribute("name", component);
            propertiesElement.addContent(componentElement);
            for (MCRProperty property : map.get(component)) {
                Element propertyElement = new Element("property");
                propertyElement.setAttribute("name", property.name());

                if (property.oldValue() != null) {
                    propertyElement.setAttribute("oldValue", property.oldValue());

                }

                propertyElement.setAttribute("newValue", property.newValue());
                propertyElement.setAttribute("component", property.component());
                componentElement.addContent(propertyElement);
            }
        }

        try {
            return getLayoutService().getTransformedContent(req, resp, new MCRJDOMContent(propertiesElement));
        } catch (TransformerException | SAXException e) {
            throw new MCRException(e);
        }
    }
}
