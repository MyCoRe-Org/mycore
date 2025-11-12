package org.mycore.mods;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.legalentity.MCRIdentifier;
import org.mycore.datamodel.legalentity.MCRLegalEntityService;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MCRUser2MODSPersonIdentifierService implements MCRLegalEntityService {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String MODSPERSON_ATTR_NAME = "id_modsperson";

    public static final String USERID = "userid";

    public static final String MODS_NAME = "name";

    public static final String MODS_NAMEIDENTIFIER = "nameIdentifier";

    public static final String TYPE = "type";


    /**
     * Gets all {@link MCRIdentifier MCRIdentifiers} of a modsperson by reference to a {@link org.mycore.user2.MCRUser}
     * and its modsperson id.
     * @param userId the user id
     * @return all known identifiers or an empty list
     */
    @Override
    public List<MCRIdentifier> getAllIdentifiers(MCRIdentifier userId) {
        Optional<MCRObject> modspersonOptional = findModspersonByUsername(userId);
        if (modspersonOptional.isEmpty()) {
            return Collections.emptyList();
        }
        MCRMODSWrapper wrapper = new MCRMODSWrapper(modspersonOptional.get());
        Element modsName = wrapper.getMODS().getChild(MODS_NAME, MCRConstants.MODS_NAMESPACE);
        if (modsName == null) {
            return Collections.emptyList();
        }
        return modsName.getChildren(MODS_NAMEIDENTIFIER, MCRConstants.MODS_NAMESPACE)
            .stream().map(e -> new MCRIdentifier(e.getAttributeValue(TYPE), e.getText()))
            .toList();
    }

    /**
     * Adds a {@link MCRIdentifier MCRIdentifiers} to a modsperson by reference to a {@link org.mycore.user2.MCRUser}
     * and its modsperson id.
     * @param userId the user id
     * @param attributeToAdd the nameIdentifier to add to the modsperson
     */
    @Override
    public void addIdentifier(MCRIdentifier userId, MCRIdentifier attributeToAdd) {
        Optional<MCRObject> modspersonOptional = findModspersonByUsername(userId);
        if (modspersonOptional.isEmpty()) {
            return;
        }
        MCRMODSWrapper wrapper = new MCRMODSWrapper(modspersonOptional.get());
        Element modsName = wrapper.getMODS().getChild(MODS_NAME, MCRConstants.MODS_NAMESPACE);
        if (modsName == null) {
            return;
        }
        Element nameIdentifier = new Element(MODS_NAMEIDENTIFIER, MCRConstants.MODS_NAMESPACE)
            .setAttribute(TYPE, attributeToAdd.getType())
            .setText(attributeToAdd.getValue());
        modsName.addContent(nameIdentifier);
        try {
            MCRMetadataManager.update(modspersonOptional.get());
        } catch (MCRAccessException | MCRPersistenceException e) {
            LOGGER.warn("Could not update modsperson object for user id {}",
                userId.getValue(), e);
        }
    }

    /**
     * Takes a username and returns an Optional with the referenced modsperson.
     * @param username the username
     * @return a nullable Optional that might contain a modsperson
     */
    private Optional<MCRObject> findModspersonByUsername(MCRIdentifier username) {
        if (username == null || !USERID.equals(username.getType())) {
            return Optional.empty();
        }
        MCRUser user = MCRUserManager.getUser(username.getValue());
        if (user == null) {
            return Optional.empty();
        }
        String modspersonId = user.getUserAttribute(MODSPERSON_ATTR_NAME);
        if (modspersonId == null) {
            return Optional.empty();
        }
        try {
            MCRObject modsperson = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(modspersonId));
            return Optional.of(modsperson);
        } catch (MCRPersistenceException e) {
            LOGGER.warn("Could not retrieve modsperson object for user id {} (modspersonId={})",
                username.getValue(), modspersonId, e);
            return Optional.empty();
        }
    }
}
