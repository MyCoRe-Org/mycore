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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.streams.MCRMD5InputStream;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.orcid2.auth.MCRORCIDOAuthClient;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.util.MCRIdentifier;
import org.xml.sax.SAXException;

/**
 * Provides utility methods.
 */
public class MCRORCIDUtils {

    private static final MCRContentTransformer T_ORCID_MODS_FILTER
        = MCRContentTransformerFactory.getTransformer("ORCIDMODSFilter");

    private static final List<String> TRUSTED_IDENTIFIER_TYPES
        = MCRConfiguration2.getString(MCRORCIDConstants.CONFIG_PREFIX + "Object.TrustedIdentifierTyps").stream()
          .flatMap(MCRConfiguration2::splitValue).collect(Collectors.toList());

    private static final List<String> PUBLISH_STATES
        = MCRConfiguration2.getString(MCRORCIDConstants.CONFIG_PREFIX + "Work.PublishStates").stream()
        .flatMap(MCRConfiguration2::splitValue).toList();

    /**
     * MD5 hashes String.
     *
     * @param input the input
     * @return hash as String
     */
    public static String hashString(String input) {
        final byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        final MessageDigest md5Digest = MCRMD5InputStream.buildMD5Digest();
        md5Digest.update(bytes);
        final byte[] digest = md5Digest.digest();
        return MCRMD5InputStream.getMD5String(digest);
    }

    /**
     * Checks if MCRObjects' state is ready to publish.
     * 
     * @param object the MCRObject
     * @return true if state is ready to publish or there is not state
     */
    public static boolean checkPublishState(MCRObject object) {
        return getStateValue(object).map(s -> checkPublishState(s)).orElse(true);
    }

    /**
     * Checks if state is ready to publish.
     * 
     * @param state the state
     * @return true if state is ready to publish
     */
    public static boolean checkPublishState(String state) {
        if (PUBLISH_STATES.contains(state)) {
            return true;
        }
        return PUBLISH_STATES.size() == 1 && PUBLISH_STATES.contains("*");
    }

    /**
     * Checks if MCRObject has empty MODS.
     * 
     * @param object the MCRObject
     * @return true if MCRObject's MODS has children
     */
    public static boolean checkEmptyMODS(MCRObject object) {
        final MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        return !wrapper.getMODS().getChildren().isEmpty();
    }

    /**
     * Filters MCRObject.
     * 
     * @param object the MCRObject
     * @return filtered MCRObject
     * @throws MCRORCIDException if filtering fails
     */
    public static MCRObject filterObject(MCRObject object) {
        try {
            final MCRContent filtertedObjectContent
                = T_ORCID_MODS_FILTER.transform(new MCRJDOMContent(object.createXML()));
            return new MCRObject(filtertedObjectContent.asXML());
        } catch (IOException | JDOMException | SAXException e) {
            throw new MCRORCIDException("Filter transformation failed", e);
        }
    }

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
     * Returns Set of MCRIdentifier based on mods:identifier for MCRMODSWrapper.
     * Trusted identifier types can be defined as follows:
     *
     * MCR.ORCID2.Object.TrustedIdentifierTypes=
     *
     * If empty, all identifiers will be returned.
     * 
     * @param wrapper the MCRMODSWrapper
     * @return Set of MCRIdentifier
     */
    public static Set<MCRIdentifier> getTrustedIdentifiers(MCRMODSWrapper wrapper) {
        Set<MCRIdentifier> identifiers = getIdentifiers(wrapper);
        if (TRUSTED_IDENTIFIER_TYPES.size() > 0) {
            identifiers = identifiers.stream().filter(i -> TRUSTED_IDENTIFIER_TYPES.contains(i.getType()))
                .collect(Collectors.toSet());
        }
        return identifiers;
    }

    /**
     * Returns List of MCRIdentifier based on mods:identifier.
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

    private static Optional<String> getStateValue(MCRObject object) {
        return Optional.ofNullable(object.getService().getState()).map(MCRCategoryID::getID);
    }
}
