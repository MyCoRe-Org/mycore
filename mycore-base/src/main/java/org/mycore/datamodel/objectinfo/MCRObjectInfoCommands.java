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

@MCRCommandGroup(name = "Object Info Commands")
public class MCRObjectInfoCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(syntax = "remove all objectinfo",
        help = "deletes the objectinfo for all objects")
    public static void deleteAllObjectInfo() {
        MCRObjectInfoEntityManager.removeAll();
    }

    @MCRCommand(syntax = "create all objectinfo",
        help = "reads all objects and creates the corresponding objectinfo",
        order = 10)
    public static List<String> createAllObjectInfo() {
        MCRXMLMetadataManager mm = MCRXMLMetadataManager.instance();
        return mm.getObjectBaseIds()
            .stream().filter(b -> !b.endsWith("derivate"))
            .map(b -> "create objectinfo for base " + b)
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "create objectinfo for base {0}",
        help = "reads all objects with base id {0} and creates the corresponding objectinfo")
    public static List<String> createObjectInfoForBase(String baseId) {
        String[] idParts = baseId.split("_");
        MCRXMLMetadataManager mm = MCRXMLMetadataManager.instance();
        if (idParts[1].equals("derivate")) {
            return List.of();
        }

        int maxID = mm.getHighestStoredID(idParts[0], idParts[1]);
        return IntStream.rangeClosed(1, maxID)
            .mapToObj(n -> MCRObjectID.formatID(baseId, n))
            .map(id -> "create objectinfo for object " + id)
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "create objectinfo for object {0}",
        help = "creates the corresponding objectinfo for MCRObject {0}")
    public static void createObjectInfoForObject(String idStr) {
        MCRObjectID id = MCRObjectID.getInstance(idStr);
        LogManager.getLogger().info("create objectinfo for object " + idStr);
        if (id.getTypeId().equals("derivate")) {
            return;
        }
        try {
            if (MCRMetadataManager.exists(id)) {
                MCRObjectInfoEntityManager.update(MCRMetadataManager.retrieveMCRObject(id));
                LogManager.getLogger().info("objectinfo for object " + idStr + " created.");

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
                        LogManager.getLogger().info("objectinfo for object " + idStr + " created.");
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
