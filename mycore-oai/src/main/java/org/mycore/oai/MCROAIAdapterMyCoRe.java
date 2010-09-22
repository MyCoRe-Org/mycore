package org.mycore.oai;

import org.apache.log4j.Logger;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRQueryCondition;

public class MCROAIAdapterMyCoRe extends MCROAIAdapter {
    private final static Logger LOGGER = Logger.getLogger(MCROAIAdapterMyCoRe.class);

    public boolean exists(String id) {
        try {
            MCRObjectID oid = new MCRObjectID(id);
            return MCRXMLMetadataManager.instance().exists(oid);
        } catch (Exception ex) {
            String msg = "Exception while checking existence of object " + id;
            LOGGER.warn(msg, ex);
            return false;
        }
    }

    public MCRCondition buildSetCondition(String setSpec) {
        String categID = setSpec.substring(setSpec.lastIndexOf(':') + 1).trim();
        String classID = setSpec.substring(0, setSpec.indexOf(':')).trim();
        return new MCRQueryCondition(MCRFieldDef.getDef(classID), "=", categID);
    }
}
