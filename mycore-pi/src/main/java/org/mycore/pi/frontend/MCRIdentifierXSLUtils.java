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

package org.mycore.pi.frontend;

import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.DOMOutputter;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.MCRPIService;
import org.mycore.pi.MCRPIServiceManager;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.w3c.dom.NodeList;

public class MCRIdentifierXSLUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    public static boolean hasIdentifierCreated(String service, String id, String additional) {
        MCRPIService<MCRPersistentIdentifier> registrationService = MCRPIServiceManager
            .getInstance().getRegistrationService(service);
        return registrationService.isCreated(MCRObjectID.getInstance(id), additional);
    }

    public static boolean hasIdentifierRegistrationStarted(String service, String id, String additional) {
        MCRPIService<MCRPersistentIdentifier> registrationService = MCRPIServiceManager
            .getInstance().getRegistrationService(service);
        return registrationService.hasRegistrationStarted(MCRObjectID.getInstance(id), additional);
    }

    public static boolean hasIdentifierRegistered(String service, String id, String additional) {
        MCRPIService<MCRPersistentIdentifier> registrationService = MCRPIServiceManager
            .getInstance().getRegistrationService(service);
        return registrationService.isRegistered(MCRObjectID.getInstance(id), additional);
    }

    public static boolean hasManagedPI(String objectID) {
        return MCRPIManager.getInstance()
            .getRegistered(MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(objectID))).size() > 0;
    }

    public static boolean isManagedPI(String pi, String id) {
        return MCRPIManager.getInstance().getInfo(pi).stream().anyMatch(info -> info.getMycoreID()
            .equals(id));
    }

    /**
     * Gets all available services which are configured.
     * e.g.
     * <ul>
     * <li>&lt;service id="service1" inscribed="false" permission="true" type="urn" /&gt;</li>
     * <li>&lt;service id="service2" inscribed="true" permission="false" type="doi" /&gt;</li>
     * </ul>
     *
     * @param objectID the object
     * @return a Nodelist
     * @throws JDOMException
     */
    public static NodeList getPIServiceInformation(String objectID) throws JDOMException {
        Element e = new Element("list");

        MCRBase obj = MCRMetadataManager.retrieve(MCRObjectID.getInstance(objectID));
        MCRPIServiceManager.getInstance().getServiceList()
            .stream()
            .map((rs -> {
                Element service = new Element("service");

                service.setAttribute("id", rs.getServiceID());

                // Check if the inscriber of this service can read a PI
                try {
                    if (rs.getMetadataService().getIdentifier(obj, "").isPresent()) {
                        service.setAttribute("inscribed", "true");
                    } else {
                        service.setAttribute("inscribed", "false");
                    }
                } catch (MCRPersistentIdentifierException e1) {
                    LOGGER.warn("Error happened while try to read PI from object: {}", objectID, e1);
                    service.setAttribute("inscribed", "false");
                }

                // rights
                String permission = "register-" + rs.getServiceID();
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
