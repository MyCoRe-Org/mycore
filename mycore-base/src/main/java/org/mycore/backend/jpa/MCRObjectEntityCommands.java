/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  MyCoRe is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MyCoRe is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.backend.jpa;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.datamodel.common.MCRAbstractMetadataVersion;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.xml.sax.SAXException;

@MCRCommandGroup(name = "Object Entity Commands")
public class MCRObjectEntityCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(syntax = "create object entities",
        help = "reads all objects and creates the corresponding entities")
    public static void createObjectEntities() {
        MCRObjectEntityManager.removeAll();

        MCRXMLMetadataManager mm = MCRXMLMetadataManager.instance();
        for (String baseId : mm.getObjectBaseIds()) {
            String[] idParts = baseId.split("_");
            int maxID = mm.getHighestStoredID(idParts[0], idParts[1]);

            if(idParts[1].equals("derivate")){
                continue; // we do not want derivates
            }

            for (int i = 1; i < maxID; i++) {
                String idStr = MCRObjectID.formatID(idParts[0], idParts[1], i);
                MCRObjectID id = MCRObjectID.getInstance(idStr);
                try {
                    if (MCRMetadataManager.exists(id)) {
                        MCRObjectEntityManager.update(MCRMetadataManager.retrieveMCRObject(id));
                    } else {
                        List<? extends MCRAbstractMetadataVersion<?>> versions = MCRXMLMetadataManager.instance()
                            .listRevisions(id);
                        if (versions == null || versions.isEmpty()) {
                            // we do not know if the object ever existed
                            LOGGER.warn("Could not determine what happened to " + id);
                        } else {
                            MCRAbstractMetadataVersion<?> deleted = versions.get(versions.size() - 1);
                            MCRAbstractMetadataVersion<?> lastExisting = versions.get(versions.size() - 2);
                            try {
                                Document doc = lastExisting.retrieve().asXML();
                                MCRObject obj = new MCRObject(doc);
                                MCRObjectEntityManager.update(obj);
                                MCRObjectEntityManager.delete(obj,
                                    deleted.getDate().toInstant(), deleted.getUser());
                            } catch (JDOMException | SAXException e) {
                                LOGGER.warn("Could not determine what happened to " + id, e);
                            }
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("Error while getting history of {}", id);
                }
            }
        }
    }
}
