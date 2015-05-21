/**
 * 
 */
package org.mycore.migration2015.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs2.MCRMetadataVersion;
import org.mycore.datamodel.ifs2.MCRVersionedMetadata;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

/**
 * @author Thomas Scheffler (yagee)
 */
@MCRCommandGroup(
    name = "MyCore 2015.0x migration")
public class MCRMigrationCommands {

    private static final Logger LOGGER = Logger.getLogger(MCRMigrationCommands.class);

    @MCRCommand(
        syntax = "migrate author servflags", help = "Create missing servflags for createdby and modifiedby. (MCR-786)",
        order = 20)
    public static List<String> addServFlags() {
        TreeSet<String> ids = new TreeSet<>(MCRXMLMetadataManager.instance().listIDs());
        ArrayList<String> cmds = new ArrayList<>(ids.size());
        for (String id : ids) {
            cmds.add("migrate author servflags for " + id);
        }
        return cmds;
    }

    @MCRCommand(
        syntax = "migrate author servflags for {0}",
        help = "Create missing servflags for createdby and modifiedby for object {0}. (MCR-786)", order = 10)
    public static void addServFlags(String id) throws IOException, MCRPersistenceException, MCRActiveLinkException {
        MCRObjectID objectID = MCRObjectID.getInstance(id);
        MCRBase obj = MCRMetadataManager.retrieve(objectID);
        MCRObjectService service = obj.getService();
        if (!service.isFlagTypeSet(MCRObjectService.FLAG_TYPE_CREATEDBY)) { //the egg
            MCRVersionedMetadata versionedMetadata = MCRXMLMetadataManager.instance().getVersionedMetaData(objectID);
            String createUser = null, modifyUser = null;
            if (versionedMetadata == null) {
                LOGGER
                    .warn("Cannot restore author servflags as there are no versions available. Setting to current user.");
                createUser = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
                modifyUser = createUser;
            } else {
                List<MCRMetadataVersion> versions = versionedMetadata.listVersions();
                MCRMetadataVersion firstVersion = versions.get(0);
                for (MCRMetadataVersion version : versions) {
                    if (version.getType() == 'A') {
                        firstVersion = version; // get last 'added'
                    }
                }
                MCRMetadataVersion lastVersion = versions.get(versions.size() - 1);
                createUser = firstVersion.getUser();
                modifyUser = lastVersion.getUser();
            }
            service.addFlag(MCRObjectService.FLAG_TYPE_CREATEDBY, createUser);
            LOGGER.info(objectID + ", created by: " + createUser);
            if (!service.isFlagTypeSet(MCRObjectService.FLAG_TYPE_MODIFIEDBY)) { //the chicken
                //have to restore also modifiedby from version history.
                LOGGER.info(objectID + ", modified by: " + modifyUser);
                service.addFlag(MCRObjectService.FLAG_TYPE_CREATEDBY, modifyUser);
            }
            obj.setImportMode(true);
            if (obj instanceof MCRDerivate) {
                MCRMetadataManager.updateMCRDerivateXML((MCRDerivate) obj);
            } else {
                MCRMetadataManager.update((MCRObject) obj);
            }
        }
    }
}
