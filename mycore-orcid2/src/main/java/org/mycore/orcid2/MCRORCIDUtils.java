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

package org.mycore.orcid2;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.orcid2.auth.MCRORCIDOAuthClient;
import org.mycore.orcid2.user.MCRIdentifier;

/**
 * Provides utility methods.
 */
public class MCRORCIDUtils {

    /**
     * Compares String with auth client's client id.
     * 
     * @param sourcePath source path
     * @return true if source path equals client id
     */
    public static boolean isCreatedByThisApplication(String sourcePath) {
        return MCRORCIDOAuthClient.CLIENT_ID.equals(sourcePath);
    }

    /**
     * Builds a modsCollection Element and adds given elements.
     * 
     * @param elements List of elements
     * @return Element containing elements
     */
    public static Element buildMODSCollection(List<Element> elements) {
        final Element modsCollection = new Element("modsCollection", MCRConstants.MODS_NAMESPACE);
        elements.forEach(w -> modsCollection.addContent(w));
        return modsCollection;
    }

    /**
     * Extracts all orcid name identifiers in mods.
     * 
     * @param object the MCRObject
     * @return Set of ORCID MCRIdentifier
     */
    public static Set<String> getORCIDs(MCRObject object) {
        return new MCRMODSWrapper(object).getElements("mods:name/mods:nameIdentifier[@type='orcid']").stream()
            .map(Element::getTextTrim).collect(Collectors.toSet());
    }

    /**
     * Lists mods:name Elements.
     * 
     * @param wrapper the MCRMODSWrapper
     * @return List of name elements
     */
    public static List<Element> listNameElements(MCRMODSWrapper wrapper) {
        return wrapper.getElements("mods:name");
    }

    /**
     * Returns mods:nameIdentifer.
     * 
     * @param nameElement the mods:name Element
     * @return Set of MCRIdentifier
     */
    public static Set<MCRIdentifier> getNameIdentifiers(Element nameElement) {
        return nameElement.getChildren("nameIdentifier", MCRConstants.MODS_NAMESPACE).stream()
            .map(e -> getIdentfierFromElement(e))
            .collect(Collectors.toSet());
    }

    /**
     * Returns mods:nameIdentifer.
     * 
     * @param wrapper the MCRMODSWrapper
     * @return Set of MCRIdentifier
     */
    public static Set<MCRIdentifier> getNameIdentifiers(MCRMODSWrapper wrapper) {
        return wrapper.getElements("mods:nameIdentifier").stream().map(e -> getIdentfierFromElement(e))
            .collect(Collectors.toSet());
    }

    /**
     * Returns mods:identifer.
     * 
     * @param wrapper the MCRMODSWrapper
     * @return Set of MCRIdentifier
     */
    public static Set<MCRIdentifier> getIdentifiers(MCRMODSWrapper wrapper) {
        return wrapper.getElements("mods:identifier").stream().map(e -> getIdentfierFromElement(e))
            .collect(Collectors.toSet());
    }

    private static MCRIdentifier getIdentfierFromElement(Element element) {
        return new MCRIdentifier(element.getAttributeValue("type"), element.getTextTrim());
    }
}
