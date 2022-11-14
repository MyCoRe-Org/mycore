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
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.orcid2.auth.MCRORCIDOAuthClient;

/**
 * Provides utility methods.
 */
public class MCRORCIDUtils {

    private static final String MATCH_ONLY_NAME_IDENTIFIER
        = MCRConfiguration2.getString(MCRORCIDConstants.CONFIG_PREFIX + "User.NameIdentifier").orElse("");

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
     * Extracts all name identfiers from mods. Name identiers are represented as key
     * value strings.
     * 
     * MCR.ORCID2.User.NameIdentifier=
     * 
     * Optionally specifies the type of names identifiers.
     * 
     * @param wrapper wrapper of mods object
     * @return set of name identifer keys
     * @see MCRORCIDUtils#buildIdentifierKey
     */
    public static Set<String> getNameIdentifierKeys(MCRMODSWrapper wrapper) {
        return getIdentifierKeys(wrapper, !MATCH_ONLY_NAME_IDENTIFIER.isEmpty()
            ? "[@type = '" + MATCH_ONLY_NAME_IDENTIFIER + "']" : "mods:name/mods:nameIdentifier");
    }

    /**
     * Extracts all identfiers from mods. Name identiers are represented as key
     * value strings.
     * 
     * @param wrapper wrapper of mods object
     * @return set of identifer keys
     * @see MCRORCIDUtils#buildIdentifierKey
     */
    public static Set<String> getIdentifierKeys(MCRMODSWrapper wrapper) {
        return getIdentifierKeys(wrapper, "mods:identifier");
    }

    /**
     * Builds identifier key.
     * 
     * @param type identifier type
     * @param value identifier value
     * @return name identifier as key value string
     */
    public static String buildIdentifierKey(String type, String value) {
        return String.format(Locale.ROOT, "%s:%s", type, value);
    }

    private static Set<String> getIdentifierKeys(MCRMODSWrapper wrapper, String xPath) {
        return getIdentifierKeys(wrapper.getElements(xPath));
    }

    private static Set<String> getIdentifierKeys(List<Element> elements) {
        return elements.stream().map(i -> buildIdentifierKey(i)).collect(Collectors.toSet());
    }

    private static String buildIdentifierKey(Element modsIdentifier) {
        return buildIdentifierKey(modsIdentifier.getAttributeValue("type"), modsIdentifier.getTextTrim());
    }
}
