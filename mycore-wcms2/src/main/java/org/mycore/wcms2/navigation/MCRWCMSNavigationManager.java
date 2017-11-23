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

package org.mycore.wcms2.navigation;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRDOMUtils;
import org.mycore.frontend.MCRLayoutUtilities;
import org.mycore.wcms2.MCRWCMSUtil;
import org.mycore.wcms2.MCRWebPagesSynchronizer;
import org.mycore.wcms2.datamodel.MCRNavigation;
import org.mycore.wcms2.datamodel.MCRNavigationBaseItem;
import org.mycore.wcms2.datamodel.MCRNavigationItem;
import org.mycore.wcms2.datamodel.MCRNavigationItemContainer;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.JsonObject;

public class MCRWCMSNavigationManager {

    private static MCRWCMSNavigationProvider NAVIGATION_PROVIDER;

    static {
        MCRConfiguration conf = MCRConfiguration.instance();
        NAVIGATION_PROVIDER = conf.getInstanceOf("MCR.WCMS2.navigationProvider",
            MCRWCMSDefaultNavigationProvider.class.getName());
    }

    /**
     * @see MCRWCMSNavigationProvider#toJSON(MCRNavigation)
     */
    public static synchronized JsonObject toJSON(MCRNavigation navigation) {
        return NAVIGATION_PROVIDER.toJSON(navigation);
    }

    public static synchronized MCRNavigation fromJSON(JsonObject jsonNavigation) {
        return NAVIGATION_PROVIDER.fromJSON(jsonNavigation);
    }

    /**
     * Saves the given navigation
     */
    public static void save(MCRNavigation navigation) throws IOException, JAXBException, JDOMException {
        OutputStream out = MCRWebPagesSynchronizer.getOutputStream(MCRLayoutUtilities.NAV_RESOURCE);
        MCRWCMSUtil.save(navigation, out);
        out.flush();
        out.close();
    }

    /**
     * Returns the navigation as json.
     */
    public static synchronized JsonObject getNavigationAsJSON() throws IOException, SAXException, JAXBException {
        return NAVIGATION_PROVIDER.toJSON(getNavigation());
    }

    /**
     * Returns the navigation as jdom document.
     */
    public static synchronized Document getNavigationAsXML() {
        return MCRLayoutUtilities.getNavi();
    }

    /**
     * Returns the navigation as pojo.
     */
    public static MCRNavigation getNavigation() throws IOException, SAXException, JAXBException {
        DocumentBuilder documentBuilder = MCRDOMUtils.getDocumentBuilderUnchecked();
        try {
            org.w3c.dom.Document doc = documentBuilder.parse(MCRLayoutUtilities.getNavigationURL().toString());
            if (doc.getElementsByTagName("menu").getLength() == 0) {
                NodeList nodeList = doc.getFirstChild().getChildNodes();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        ((org.w3c.dom.Element) nodeList.item(i)).setAttribute("id", nodeList.item(i).getNodeName());
                        doc.renameNode(nodeList.item(i), null, "menu");
                    }
                }
            } else {
                MCRConfiguration.instance().set("MCR.NavigationFile.SaveInOldFormat", false);
            }
            return MCRWCMSUtil.load(doc);
        } finally {
            MCRDOMUtils.releaseDocumentBuilder(documentBuilder);
        }
    }

    /**
     * Runs recursive through the item tree and changes each href and hrefStartingPage attribute to the new href.
     * 
     * @param item
     *            navigation item to change (and all its children)
     * @param from
     *            which href to change
     * @param to
     *            new value of href
     * @return if something in the tree was changed
     */
    public static boolean updateHref(MCRNavigationBaseItem item, String from, String to) {
        boolean dirty = false;
        if (item instanceof MCRNavigation) {
            MCRNavigation navigation = (MCRNavigation) item;
            if (navigation.getHrefStartingPage() != null && navigation.getHrefStartingPage().equals(from)) {
                navigation.setHrefStartingPage(to);
                dirty = true;
            }
        } else if (item instanceof MCRNavigationItem) {
            MCRNavigationItem navItem = (MCRNavigationItem) item;
            if (navItem.getHref().equals(from)) {
                navItem.setHref(to);
                dirty = true;
            }
        }
        if (item instanceof MCRNavigationItemContainer) {
            MCRNavigationItemContainer container = (MCRNavigationItemContainer) item;
            for (MCRNavigationBaseItem child : container.getChildren()) {
                if (updateHref(child, from, to)) {
                    dirty = true;
                }
            }
        }
        return dirty;
    }
}
