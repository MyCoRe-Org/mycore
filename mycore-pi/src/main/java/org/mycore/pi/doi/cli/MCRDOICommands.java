package org.mycore.pi.doi.cli;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.doi.MCRDOIRegistrationService;
import org.mycore.pi.doi.MCRDataciteClient;
import org.mycore.pi.doi.MCRDigitalObjectIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

@MCRCommandGroup(name = "DOI Commands")
public class MCRDOICommands {

    private static final Logger LOGGER = LogManager.getLogger();


    @MCRCommand(syntax = "repair registered dois {0}", help = "Contacts the Registration Service and inserts all registered DOIs to the Database. It also updates all media files. The Service ID is the id from the configuration.", order = 10)
    public static void synchronizeDatabase(String serviceID) {
        MCRDOIRegistrationService registrationService = new MCRDOIRegistrationService(serviceID);

        try {
            MCRDataciteClient dataciteClient = registrationService.getDataciteClient();
            List<MCRDigitalObjectIdentifier> doiList = dataciteClient.getDOIList();

            doiList.stream().forEach(doi -> {
                try {
                    URI uri = dataciteClient.resolveDOI(doi);

                    if (uri.toString().startsWith(registrationService.getRegisterURL())) {
                        String s = uri.toString();
                        LOGGER.info("Checking DOI: " + doi.asString() + " / " + s);

                        String idString = s.substring(s.lastIndexOf("/") + 1);

                        MCRObjectID objectID = MCRObjectID.getInstance(idString);
                        if(MCRMetadataManager.exists(objectID)){
                            if (!registrationService.isRegistered(objectID, "")) {
                                LOGGER.info("DOI is not registered in MyCoRe. Add to Database: " + doi.asString() + " / " + s);

                                MCRPI databaseEntry = new MCRPI(doi.asString(), registrationService.getType(), idString, "", serviceID, new Date());
                                MCRHIBConnection.instance().getSession().save(databaseEntry);
                            }

                            // Update main files
                            MCRObject obj = MCRMetadataManager.retrieveMCRObject(objectID);
                            List<Map.Entry<String, URI>> entryList = registrationService.getMediaList(obj);
                            dataciteClient.setMediaList(doi, entryList);
                        } else {
                            LOGGER.info("Could not find Object : " + objectID.toString());
                        }
                    } else {
                        LOGGER.info("DOI/URL is not from this application: " + doi.asString() + "/" + uri.toString());
                    }
                } catch (MCRPersistentIdentifierException e) {
                    LOGGER.error("Error occurred for DOI: " + doi.toString(), e);
                }
            });
        } catch (MCRPersistentIdentifierException e) {
            LOGGER.error("Error while receiving DOI list from Registration-Service!", e);
        }
    }
}
