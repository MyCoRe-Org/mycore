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

package org.mycore.pi.doi.cli;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.pi.MCRPersistentIdentifierMetadataManager;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.doi.MCRDOIParser;
import org.mycore.pi.doi.MCRDOIRegistrationService;
import org.mycore.pi.doi.MCRDataciteClient;
import org.mycore.pi.doi.MCRDigitalObjectIdentifier;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

@MCRCommandGroup(name = "DOI Commands")
public class MCRDOICommands {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String REPAIR_MEDIALIST_OF_0_AND_SERVICE_1 = "repair medialist of {0} and service {1}";

    @MCRCommand(syntax = "repair incomplete registered doi {0} with registration service {1}",
        help = "Use this method if a DOI is registered, but not inserted in the Database. {0} is the DOI and "
            + "{1} the registration service from configuration.")
    public static void repairIncompleteRegisteredDOI(String doiString, String serviceID)
        throws MCRPersistentIdentifierException, MCRAccessException, MCRActiveLinkException {
        MCRDOIRegistrationService registrationService = new MCRDOIRegistrationService(serviceID);
        MCRDataciteClient dataciteClient = registrationService.getDataciteClient();

        MCRDigitalObjectIdentifier doi = new MCRDOIParser().parse(doiString)
            .orElseThrow(() -> new MCRException("Invalid DOI: " + doiString));

        URI uri = dataciteClient.resolveDOI(doi);
        if (!uri.toString().startsWith(registrationService.getRegisterURL())) {
            LOGGER.info("DOI/URL is not from this application: {}/{}", doi.asString(), uri);
            return;
        }

        MCRObjectID objectID = getObjectID(uri);
        if (!MCRMetadataManager.exists(objectID)) {
            LOGGER.info("Could not find Object : {}", objectID);
            return;
        }

        MCRObject mcrObject = MCRMetadataManager.retrieveMCRObject(objectID);
        MCRPersistentIdentifierMetadataManager<MCRDigitalObjectIdentifier> synchronizer = registrationService
            .getMetadataManager();

        if (!registrationService.isRegistered(objectID, doiString)) {
            LOGGER.info("{} is not found in PI-Database. Insert it..", objectID);
            registrationService.insertIdentifierToDatabase(mcrObject, "", doi);
        }

        if (!synchronizer.getIdentifier(mcrObject, "").isPresent()) {
            LOGGER.info("Object doesn't have Identifier inscribed! Insert it..");
            synchronizer.insertIdentifier(doi, mcrObject, "");
            MCRMetadataManager.update(mcrObject);
        }

    }

    @MCRCommand(syntax = "repair registered dois {0}",
        help = "Contacts the Registration Service and inserts all registered DOIs to the Database. It also updates all media files. The Service ID{0} is the id from the configuration.",
        order = 10)
    public static void synchronizeDatabase(String serviceID) {
        MCRDOIRegistrationService registrationService = new MCRDOIRegistrationService(serviceID);

        try {
            MCRDataciteClient dataciteClient = registrationService.getDataciteClient();
            List<MCRDigitalObjectIdentifier> doiList = dataciteClient.getDOIList();

            doiList.stream().filter(doi -> {
                boolean isTestDOI = doi.getPrefix().equals(MCRDigitalObjectIdentifier.TEST_DOI_PREFIX);
                return !isTestDOI || registrationService.usesTestPrefix();
            }).forEach(doi -> {
                try {
                    URI uri = dataciteClient.resolveDOI(doi);

                    if (uri.toString().startsWith(registrationService.getRegisterURL())) {
                        LOGGER.info("Checking DOI: {}", doi.asString());
                        MCRObjectID objectID = getObjectID(uri);
                        if (MCRMetadataManager.exists(objectID)) {
                            if (!registrationService.isRegistered(objectID, "")) {
                                LOGGER.info("DOI is not registered in MyCoRe. Add to Database: {}", doi.asString());
                                MCRPI databaseEntry = new MCRPI(doi.asString(), registrationService.getType(),
                                    objectID.toString(), "", serviceID, new Date());
                                MCRHIBConnection.instance().getSession().save(databaseEntry);
                            }

                            // Update main files
                            MCRObject obj = MCRMetadataManager.retrieveMCRObject(objectID);
                            List<Map.Entry<String, URI>> entryList = registrationService.getMediaList(obj);
                            dataciteClient.setMediaList(doi, entryList);
                        } else {
                            LOGGER.info("Could not find Object : {}", objectID);
                        }
                    } else {
                        LOGGER.info("DOI/URL is not from this application: {}/{}", doi.asString(), uri);
                    }
                } catch (MCRPersistentIdentifierException e) {
                    LOGGER.error("Error occurred for DOI: {}", doi, e);
                }
            });
        } catch (MCRPersistentIdentifierException e) {
            LOGGER.error("Error while receiving DOI list from Registration-Service!", e);
        }
    }

    private static MCRObjectID getObjectID(URI uri) {
        String s = uri.toString();
        String idString = s.substring(s.lastIndexOf("/") + 1);

        return MCRObjectID.getInstance(idString);
    }

    @MCRCommand(syntax = "repair media list of {0}",
        help = "Sends new media lists to Datacite. The Service ID{0} is the id from the configuration.")
    public static List<String> updateMediaListOfAllDOI(String serviceID) {
        MCRDOIRegistrationService registrationService = new MCRDOIRegistrationService(serviceID);

        try {
            MCRDataciteClient dataciteClient = registrationService.getDataciteClient();
            List<MCRDigitalObjectIdentifier> doiList = dataciteClient.getDOIList();

            return doiList.stream()
                .filter(doi -> {
                    boolean isTestDOI = doi.getPrefix().equals(MCRDigitalObjectIdentifier.TEST_DOI_PREFIX);
                    return !isTestDOI || registrationService.usesTestPrefix();
                })
                .map(MCRDigitalObjectIdentifier::asString)
                .map(doiStr -> MessageFormat.format(REPAIR_MEDIALIST_OF_0_AND_SERVICE_1, doiStr, serviceID))
                .collect(Collectors.toList());
        } catch (MCRPersistentIdentifierException e) {
            LOGGER.error("Error while receiving DOI list from Registration-Service!", e);
        }

        return Collections.emptyList();
    }

    @MCRCommand(syntax = REPAIR_MEDIALIST_OF_0_AND_SERVICE_1,
        help = "Sends new media list to Datacite. {0} is the DOI. The Service ID{1} is the id from the configuration.")
    public static void updateMediaListForDOI(String doiString, String serviceID) {
        MCRDOIRegistrationService registrationService = new MCRDOIRegistrationService(serviceID);

        MCRDataciteClient dataciteClient = registrationService.getDataciteClient();

        MCRDigitalObjectIdentifier doi = new MCRDOIParser()
            .parse(doiString)
            .orElseThrow(() -> new IllegalArgumentException("The String " + doiString + " is no valid DOI!"));

        try {
            URI uri = dataciteClient.resolveDOI(doi);

            if (uri.toString().startsWith(registrationService.getRegisterURL())) {
                String s = uri.toString();
                LOGGER.info("Checking DOI: {} / {}", doi.asString(), s);
                String idString = s.substring(s.lastIndexOf("/") + 1);

                MCRObjectID objectID = MCRObjectID.getInstance(idString);
                if (MCRMetadataManager.exists(objectID)) {
                    MCRObject obj = MCRMetadataManager.retrieveMCRObject(objectID);
                    List<Map.Entry<String, URI>> newMediaList = registrationService.getMediaList(obj);

                    List<Map.Entry<String, URI>> oldMediaList;
                    try {
                        oldMediaList = dataciteClient.getMediaList(doi);
                    } catch (MCRIdentifierUnresolvableException e) {
                        LOGGER.warn("{} had no media list!", doi);
                        oldMediaList = new ArrayList<>();
                    }

                    HashMap<String, URI> newHashMap = new HashMap<>();

                    newMediaList.forEach(e -> newHashMap.put(e.getKey(), e.getValue()));
                    oldMediaList.forEach(e -> {
                        /*
                        Currently it is not possible to delete inserted values key values (mime types).
                        So we update old media mimetypes which are not present in new list to the same URL of the first
                        mimetype entry.
                        */
                        if (!newHashMap.containsKey(e.getKey())) {
                            newHashMap.put(e.getKey(), newMediaList.stream()
                                .findFirst()
                                .orElseThrow(() -> new MCRException("new media list is empty (this should not happen)"))
                                .getValue());
                        }
                    });

                    dataciteClient.setMediaList(doi, newHashMap.entrySet().stream().collect(Collectors.toList()));
                    LOGGER.info("Updated media-list of {}", doiString);
                } else {
                    LOGGER.info("Object {} does not exist in this application!", objectID);
                }
            } else {
                LOGGER.info("DOI is not from this application: ({}) {}", uri, registrationService.getRegisterURL());
            }
        } catch (MCRPersistentIdentifierException e) {
            LOGGER.error("Error occurred for DOI: {}", doi, e);
        }
    }
}
