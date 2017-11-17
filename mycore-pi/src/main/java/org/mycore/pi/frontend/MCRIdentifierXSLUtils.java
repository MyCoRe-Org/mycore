package org.mycore.pi.frontend;

import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.DOMOutputter;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIRegistrationService;
import org.mycore.pi.MCRPIRegistrationServiceManager;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.MCRPersistentIdentifierManager;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.w3c.dom.NodeList;

public class MCRIdentifierXSLUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    public static boolean hasIdentifierCreated(String service, String id, String additional) {
        MCRPIRegistrationService<MCRPersistentIdentifier> registrationService = MCRPIRegistrationServiceManager
            .getInstance().getRegistrationService(service);
        return registrationService.isCreated(MCRObjectID.getInstance(id), additional);
    }

    public static boolean hasIdentifierRegistered(String service, String id, String additional) {
        MCRPIRegistrationService<MCRPersistentIdentifier> registrationService = MCRPIRegistrationServiceManager
            .getInstance().getRegistrationService(service);
        return registrationService.isRegistered(MCRObjectID.getInstance(id), additional);
    }

    public static boolean hasManagedPI(String objectID) {
        return MCRPersistentIdentifierManager.getInstance()
            .getRegistered(MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(objectID))).size() > 0;
    }

    public static boolean isManagedPI(String pi, String id) {
        return MCRPersistentIdentifierManager.getInstance().getInfo(pi).stream().anyMatch(info -> info.getMycoreID()
            .equals(id));
    }

    /**
     * Gets all available services which are configured.
     * e.g.
     *   <ul>
     *     <li>&lt;service id="service1" inscribed="false" permission="true" type="urn" /&gt;</li>
     *     <li>&lt;service id="service2" inscribed="true" permission="false"type="doi" /&gt;</li>
     *   </ul>
     *
     * @param objectID the object
     * @return a Nodelist
     * @throws JDOMException
     */
    public static NodeList getPIServiceInformation(String objectID) throws JDOMException {
        Element e = new Element("list");

        MCRBase obj = MCRMetadataManager.retrieve(MCRObjectID.getInstance(objectID));
        MCRConfiguration.instance().getPropertiesMap("MCR.PI.Registration.")
            .keySet()
            .stream()
            .map(s -> s.substring("MCR.PI.Registration.".length()))
            .filter(id -> !id.contains("."))
            .map((serviceID) -> MCRPIRegistrationServiceManager.getInstance().getRegistrationService(serviceID))
            .map((rs -> {
                Element service = new Element("service");

                service.setAttribute("id", rs.getRegistrationServiceID());

                // Check if the inscriber of this service can read a PI
                try {
                    if (rs.getMetadataManager().getIdentifier(obj, "").isPresent()) {
                        service.setAttribute("inscribed", "true");
                    } else {
                        service.setAttribute("inscribed", "false");
                    }
                } catch (MCRPersistentIdentifierException e1) {
                    LOGGER.warn("Error happened while try to read PI from object: {}", objectID, e1);
                    service.setAttribute("inscribed", "false");
                }

                // rights
                String permission = "register-" + rs.getRegistrationServiceID();
                Boolean canRegister = MCRAccessManager.checkPermission(objectID, "writedb") &&
                    MCRAccessManager.checkPermission(obj.getId(), permission);

                service.setAttribute("permission", canRegister.toString().toLowerCase(Locale.ROOT));

                // add the type
                service.setAttribute("type", rs.getType());

                return service;
            }))
            .forEach(e::addContent);
        return new DOMOutputter().output(e).getElementsByTagName("service");
    }

}
