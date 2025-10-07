package org.mycore.orcid2.user;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.util.MCRIdentifier;
import org.mycore.user2.MCRUser;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MCRModspersonORCIDIDAttributeHandler implements MCRORCIDIDAttributeHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String X_PATH_MODS_NAME = "mods:name[@type='personal']";

    @Override
    public void addORCID(String orcid, MCRUser user) throws MCRAccessException {
        MCRObject modsperson = getModspersonFromUser(user);

        if (modsperson == null) {
            throw new MCRORCIDException("No modsperson could be found for the user " + user.getUserID() + "!");
        }
        MCRMODSWrapper wrapper = new MCRMODSWrapper(modsperson);
        Element personName = wrapper.getElement(X_PATH_MODS_NAME);
        personName.addContent(new Element("nameIdentifier", MCRConstants.MODS_NAMESPACE)
            .setAttribute("type", "orcid").setText(orcid));
        MCRMetadataManager.update(modsperson);
    }

    @Override
    public Set<String> getORCIDs(MCRUser user) {
        return getNameIdentifierElements(user).filter(el -> "orcid".equals(el.getAttributeValue("type")))
            .map(Element::getText).collect(Collectors.toSet());
    }

    @Override
    public Set<MCRIdentifier> getIdentifiers(MCRUser user) {
        return getNameIdentifierElements(user).map(a -> new MCRIdentifier(a.getAttributeValue("type"),
            a.getText())).collect(Collectors.toSet());
    }

    /**
     * @param user the given {@link MCRUser}
     * @return the modsperson referenced by a user via the "id_modsperson" attribute, or null if no reference found
     */
    private MCRObject getModspersonFromUser(MCRUser user) {
        String modspersonId = user.getUserAttribute("id_modsperson");
        if (modspersonId == null) {
            LOGGER.warn("No modsperson could be found for the user {}!",
                user::getUserID);
            return null;
        }
        return MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(modspersonId));
    }

    /**
     * @param user the given {@link MCRUser}
     * @return a stream of all nameIdentifier {@link Element elements} found in the modsperson referenced by a user,
     * or an empty stream if no modsperson can be identified.
     */
    private Stream<Element> getNameIdentifierElements(MCRUser user) {
        MCRObject modsperson = getModspersonFromUser(user);
        if (modsperson == null) {
            return Stream.empty();
        }
        MCRMODSWrapper wrapper = new MCRMODSWrapper(modsperson);
        Element personName = wrapper.getElement(X_PATH_MODS_NAME);
        if (personName == null) {
            return Stream.empty();
        }
        return personName.getChildren("nameIdentifier", MCRConstants.MODS_NAMESPACE).stream();
    }
}
