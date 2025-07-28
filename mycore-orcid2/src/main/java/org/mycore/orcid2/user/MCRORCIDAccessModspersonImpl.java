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
import org.mycore.orcid2.util.MCRIdentifier;
import org.mycore.user2.MCRUser;

import java.util.Set;
import java.util.stream.Collectors;

public class MCRORCIDAccessModspersonImpl implements MCRORCIDAccess {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void addORCID(String orcid, MCRUser user) throws MCRAccessException {
        MCRObject modsperson = getModspersonFromUser(user);

        if (modsperson != null) {
            MCRMODSWrapper wrapper = new MCRMODSWrapper(modsperson);

            Element personName = wrapper.getElement("mods:name[@type='personal']");
            personName.addContent(new Element("nameIdentifier", MCRConstants.MODS_NAMESPACE)
                .setAttribute("type", "orcid").setText(orcid));
            MCRMetadataManager.update(modsperson);
        }
    }

    @Override
    public Set<String> getORCIDs(MCRUser user) {
        MCRObject modsperson = getModspersonFromUser(user);

        if (modsperson != null) {
            MCRMODSWrapper wrapper = new MCRMODSWrapper(modsperson);
            Element personName = wrapper.getElement("mods:name[@type='personal']");
            return personName.getChildren("nameIdentifier", MCRConstants.MODS_NAMESPACE)
                .stream().filter(el -> "orcid".equals(el.getAttributeValue("type")))
                .map(Element::getText).collect(Collectors.toSet());
        }
        return Set.of();
    }

    @Override
    public Set<MCRIdentifier> getIdentifiers(MCRUser user) {
        MCRObject modsperson = getModspersonFromUser(user);

        if (modsperson != null) {
            MCRMODSWrapper wrapper = new MCRMODSWrapper(modsperson);
            Element personName = wrapper.getElement("mods:name[@type='personal']");
            return personName.getChildren("nameIdentifier", MCRConstants.MODS_NAMESPACE)
                .stream().map(a -> new MCRIdentifier(a.getAttributeValue("type"), a.getText()))
                .collect(Collectors.toSet());
        }
        return Set.of();
    }

    private MCRObject getModspersonFromUser(MCRUser user) {
        String modspersonId = user.getUserAttribute("id_modsperson");
        if (modspersonId == null) {
            LOGGER.warn("For the user {} no modsperson could be found!",
                user.getUserID());
            return null;
        }
        return MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(modspersonId));
    }
}
