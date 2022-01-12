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

package org.mycore.ocfl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs2.MCRMetadataVersion;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.xml.sax.SAXException;

public class MCROCFLMigration {

    private static final Logger LOGGER = LogManager.getLogger();

    private final MCROCFLXMLMetadataManager target;

    private final ArrayList<String> invalidState;

    private final ArrayList<String> withoutHistory;

    private final ArrayList<String> success;

    private final ArrayList<String> failed;

    public MCROCFLMigration(String newRepoKey) {
        target = new MCROCFLXMLMetadataManager();
        target.setRepositoryKey(newRepoKey);

        invalidState = new ArrayList<>();
        withoutHistory = new ArrayList<>();
        success = new ArrayList<>();
        failed = new ArrayList<>();
    }

    public ArrayList<String> getInvalidState() {
        return invalidState;
    }

    public ArrayList<String> getWithoutHistory() {
        return withoutHistory;
    }

    public ArrayList<String> getSuccess() {
        return success;
    }

    public ArrayList<String> getFailed() {
        return failed;
    }

    /*
    public void start() {
        MCRXMLMetadataManager instance = MCRXMLMetadataManager.instance();
        List<String> ids = instance.listIDs();
    
        for (String id : ids) {
            LOGGER.info("Migrate {}", id);
            migrateID(id);
        }
    } */

    public void start() {
        MCRXMLMetadataManager instance = MCRXMLMetadataManager.instance();
        for (String baseId : instance.getObjectBaseIds()) {
            String[] idParts = baseId.split("_");
            int maxId = instance.getHighestStoredID(idParts[0], idParts[1]);
            List<String> possibleIds = IntStream.rangeClosed(1, maxId)
                .mapToObj(i -> MCRObjectID.formatID(baseId, i))
                .collect(Collectors.toList());

            for (String id : possibleIds) {
                LOGGER.info("Try migrate {}", id);
                migrateID(id);
            }
        }

    }

    private void migrateID(String id) {
        List<MCRMetadataVersion> revisions;
        MCRObjectID objectID = MCRObjectID.getInstance(id);
        revisions = readRevisions(objectID);
        List<MigrationStep> steps = new ArrayList<>();
        if (revisions != null) {
            try {
                for (MCRMetadataVersion rev : revisions) {
                    MigrationStep step = migrateRevision(rev, objectID);
                    steps.add(step);
                }
            } catch (IOException e) {
                // an error happened, so all steps are useless
                LOGGER.warn("Error while receiving all information which are needed to migrate the object " + id, e);
                steps.clear();
            }
        }
        if (steps.size() > 0) {
            // try version migration
            try {
                for (MigrationStep step : steps) {
                    step.execute();
                }
                success.add(id);
                return;
            } catch (Exception e) {
                // invalid state now
                LOGGER.warn("Error while migrating " + id, e);
                invalidState.add(id);
                return;
            }
        }

        MCRXMLMetadataManager instance = MCRXMLMetadataManager.instance();

        // does it even exist?
        if (instance.exists(objectID)) {
            // try without versions

            MCRJDOMContent jdomContent;
            long lastModified;

            try {
                MCRContent mcrContent = instance.retrieveContent(objectID);
                jdomContent = new MCRJDOMContent(mcrContent.asXML());
                lastModified = instance.getLastModified(objectID);
            } catch (IOException | JDOMException | SAXException e) {
                // can not even read the object
                LOGGER.warn("Error while migrating " + id, e);
                failed.add(id);
                return;
            }

            target.create(objectID, jdomContent, new Date(lastModified));
            withoutHistory.add(id);
        }
    }

    private MigrationStep migrateRevision(MCRMetadataVersion rev, MCRObjectID objectID) throws IOException {
        String user = rev.getUser();
        Date date = rev.getDate();
        LOGGER.info("Migrate revision {} of {}", rev.getRevision(), objectID);

        switch (rev.getType()) {
            case 'A':
                return new CreateMigrationStep(retriveActualContent(rev), user, date, objectID);
            case 'D':
                return new DeleteMigrationStep(user, date, objectID);
            case 'M':
                return new UpdateMigrationStep(retriveActualContent(rev), user, date, objectID);
            default:
                return null;
        }
    }

    private MCRContent retriveActualContent(MCRMetadataVersion rev) throws IOException {
        MCRContent content = rev.retrieve();
        Document document;

        try {
            document = content.asXML();
        } catch (JDOMException | SAXException e) {
            throw new IOException("Error while reading as as XML", e);
        }
        return new MCRJDOMContent(document);
    }

    private List<MCRMetadataVersion> readRevisions(MCRObjectID objectID) {
        List<MCRMetadataVersion> revisions = null;
        MCRXMLMetadataManager instance = MCRXMLMetadataManager.instance();

        try {
            revisions = (List<MCRMetadataVersion>) instance.listRevisions(objectID);
        } catch (IOException e) {
            LOGGER.error("Could not read revisions of {}", objectID, e);
        }
        return revisions;
    }

    private abstract static class MigrationStep {
        MCRContent content;

        String user;

        Date date;

        MCRObjectID objectID;

        MigrationStep(MCRContent content, String user, Date date, MCRObjectID objectID) {
            this.content = content;
            this.user = user;
            this.date = date;
            this.objectID = objectID;
        }

        public abstract void execute();
    }

    private class CreateMigrationStep extends MigrationStep {
        CreateMigrationStep(MCRContent content, String user, Date date, MCRObjectID objectID) {
            super(content, user, date, objectID);
        }

        @Override
        public void execute() {
            target.create(objectID, content, date, user);
        }
    }

    private class UpdateMigrationStep extends MigrationStep {
        UpdateMigrationStep(MCRContent content, String user, Date date, MCRObjectID objectID) {
            super(content, user, date, objectID);
        }

        @Override
        public void execute() {
            target.update(objectID, content, date, user);
        }
    }

    private class DeleteMigrationStep extends MigrationStep {
        DeleteMigrationStep(String user, Date date, MCRObjectID objectID) {
            super(null, user, date, objectID);
        }

        @Override
        public void execute() {
            target.delete(objectID, date, user);
        }
    }

}
