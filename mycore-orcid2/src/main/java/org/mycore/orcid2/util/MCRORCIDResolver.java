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

package org.mycore.orcid2.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.user.MCRORCIDSessionUtils;
import org.mycore.orcid2.user.MCRORCIDUser;

public class MCRORCIDResolver implements URIResolver {

    /**
     * Attempts to find a trusted ID match between the current user and the specified object.
     * If a match is found, returns an XML `Source` containing all ORCID IDs associated with
     * the current user; otherwise, an empty XML `Source` is returned.
     *
     * Expected syntax for `href`: <code>findOrcidsForCurrentUser:{objectId}</code>.
     *
     * @param href the URI containing the object ID to check
     * @param base this parameter is not used
     * @return an XML `Source` containing a list of ORCID IDs if a trusted ID match is found;
     *         otherwise, an empty XML `Source`
     * @throws IllegalArgument if `href` has an invalid syntax or if the specified object does not exist
     */
    @Override
    public Source resolve(String href, String base) {
        final MCRObjectID objectId = extractObjectIdFromHref(href);
        if (!MCRMetadataManager.exists(objectId)) {
            throw new IllegalArgumentException(objectId.toString() + " does not exist");
        }
        final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
        final Set<String> currentUserOrcids = orcidUser.getORCIDs();
        if (currentUserOrcids.isEmpty()) {
            return createEmptyResultSource();
        }
        final Set<MCRIdentifier> currentUserTrustedIds = orcidUser.getTrustedIdentifiers();
        if (currentUserTrustedIds.isEmpty()) {
            return createEmptyResultSource();
        }
        final MCRObject object = MCRMetadataManager.retrieveMCRObject(objectId);
        final Set<MCRIdentifier> objectNameIds = listNameIds(object);
        if (currentUserTrustedIds.removeAll(objectNameIds)) {
            return createResultSource(new ArrayList<>(currentUserOrcids));
        }
        return createEmptyResultSource();
    }

    private MCRObjectID extractObjectIdFromHref(String href) {
        final String[] split = href.split(":", 2);
        if (split.length < 2) {
            throw new IllegalArgumentException("Invalid href format. Expected ':' in href: " + href);
        }
        final String objectIdString = split[1];
        if (!MCRObjectID.isValid(objectIdString)) {
            throw new IllegalArgumentException("Object id '" + objectIdString + "' is invalid.");
        }
        return MCRObjectID.getInstance(objectIdString);
    }

    public static Set<MCRIdentifier> listNameIds(MCRObject object) {
        return MCRORCIDUtils.getNameIdentifiers(new MCRMODSWrapper(object));
    }

    private Source createEmptyResultSource() {
        return createResultSource(Collections.emptyList());
    }

    private Source createResultSource(List<String> orcids) {
        final Element orcidsElement = new Element("orcids");
        orcids.stream().map(o -> {
            final Element orcid = new Element("orcid");
            orcid.setText(o);
            return orcid;
        }).forEach(orcidsElement::addContent);
        return new JDOMSource(orcidsElement);
    }

}
