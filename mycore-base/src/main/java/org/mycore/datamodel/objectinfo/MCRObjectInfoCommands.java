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

package org.mycore.datamodel.objectinfo;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
public class MCRObjectInfoCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(syntax = "remove object entities",
        help = "removes all object entities")
    public static void deleteEntities() {
        MCRObjectInfoEntityManager.removeAll();
    }

    @MCRCommand(syntax = "create object entities",
        help = "reads all objects and creates the corresponding entities",
        order = 10)
    public static List<String> createObjectEntities() {
        MCRXMLMetadataManager mm = MCRXMLMetadataManager.instance();
        return mm.getObjectBaseIds()
            .stream().filter(b -> !b.endsWith("derivate"))
            .map(b -> "create object entities for base " + b)
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "create object entities for base {0}",
        help = "reads all objects with base id {0} and creates the corresponding entities")
    public static List<String> createObjectEntities(String baseId) {
        String[] idParts = baseId.split("_");
        MCRXMLMetadataManager mm = MCRXMLMetadataManager.instance();
        if (idParts[1].equals("derivate")) {
            return List.of();
        }

        int maxID = mm.getHighestStoredID(idParts[0], idParts[1]);
        return IntStream.rangeClosed(1, maxID)
            .mapToObj(n -> MCRObjectID.formatID(baseId, n))
            .map(id -> "create object entity for object " + id)
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "create object entity for object {0}",
        help = "creates the corresponding entity for MCRObject {0}")
    public static void createObjectEntity(String idStr) {
        MCRObjectID id = MCRObjectID.getInstance(idStr);
        LogManager.getLogger().info("create entity for object " + idStr);
        if (id.getTypeId().equals("derivate")) {
            return;
        }
        try {
            if (MCRMetadataManager.exists(id)) {
                MCRObjectInfoEntityManager.update(MCRMetadataManager.retrieveMCRObject(id));
                LogManager.getLogger().info("object entity for object " + idStr + " created.");

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
                        MCRObjectInfoEntityManager.update(obj);
                        MCRObjectInfoEntityManager.delete(obj,
                            deleted.getDate().toInstant(), deleted.getUser());
                        LogManager.getLogger().info("object entity for object " + idStr + " created.");
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
