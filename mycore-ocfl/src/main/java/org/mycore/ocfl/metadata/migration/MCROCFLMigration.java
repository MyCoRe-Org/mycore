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

package org.mycore.ocfl.metadata.migration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.common.MCRAbstractMetadataVersion;
import org.mycore.datamodel.common.MCRMetadataVersionType;
import org.mycore.datamodel.common.MCRObjectIDGenerator;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.ocfl.metadata.MCROCFLXMLMetadataManager;

public class MCROCFLMigration {

    private static final Logger LOGGER = LogManager.getLogger();

    private final MCROCFLXMLMetadataManager target;

    private final ArrayList<String> invalidState;

    private final ArrayList<String> withoutHistory;

    private final ArrayList<String> success;

    private final ArrayList<String> failed;

    private final List<MCROCFLRevisionPruner> pruners;

    public MCROCFLMigration(String newRepoKey) {
        this(newRepoKey, new ArrayList<>());
    }

    public MCROCFLMigration(String newRepoKey, List<MCROCFLRevisionPruner> pruners) {
        this(newRepoKey, pruners, new MCROCFLXMLMetadataManager());
    }

    public MCROCFLMigration(String newRepoKey, List<MCROCFLRevisionPruner> pruners, MCROCFLXMLMetadataManager target) {
        this.target = target;

        if (newRepoKey != null) {
            target.setRepositoryKey(newRepoKey);
        }

        invalidState = new ArrayList<>();
        withoutHistory = new ArrayList<>();
        success = new ArrayList<>();
        failed = new ArrayList<>();
        this.pruners = pruners;
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

    public void start() {
        MCRObjectIDGenerator mcrObjectIDGenerator = MCRMetadataManager.getMCRObjectIDGenerator();

        for (String baseId : mcrObjectIDGenerator.getBaseIDs()) {
            final MCRObjectID lastId = mcrObjectIDGenerator.getLastID(baseId);

            if (null == lastId) {
                LOGGER.warn("No ids found for base {}.", baseId);
                continue;
            }

            final int maxId = lastId.getNumberAsInteger();
            List<String> possibleIds = IntStream.rangeClosed(1, maxId)
                .mapToObj(i -> MCRObjectID.formatID(baseId, i))
                .toList();

            for (String id : possibleIds) {
                LOGGER.info("Try migrate {}", id);
                migrateID(id);
            }
        }

    }


    private void migrateID(String id) {
        List<? extends MCRAbstractMetadataVersion<?>> revisions;
        MCRObjectID objectID = MCRObjectID.getInstance(id);
        revisions = readRevisions(objectID);
        List<MCROCFLRevision> steps = new ArrayList<>();
        if (revisions != null) {
            try {
                for (MCRAbstractMetadataVersion<?> rev : revisions) {
                    MCROCFLRevision step = migrateRevision(rev, objectID);

                    // read one time now, to avoid errors later, but do not store it
                    retriveActualContent(rev);

                    steps.add(step);
                }
            } catch (Exception e) {
                // an error happened, so all steps are useless
                LOGGER.warn("Error while receiving all information which are needed to migrate the object " + id, e);
                steps.clear();
            }
        }

        int originalStepsSize = steps.size();
        for (MCROCFLRevisionPruner pruner : pruners) {
            try {
                int size = steps.size();
                steps = pruner.prune(steps);
                LOGGER.info("Pruned {} revisions to {} with {}", size, steps.size(), pruner);
            } catch (IOException | JDOMException e) {
                LOGGER.warn("Error while pruning " + id, e);
                failed.add(id);
                return;
            }
        }


        if (originalStepsSize > 0) {
            // try version migration
            try {
                for (MCROCFLRevision step : steps) {
                    writeRevision(step, objectID);
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
            } catch (IOException | JDOMException e) {
                // can not even read the object
                LOGGER.warn("Error while migrating " + id, e);
                failed.add(id);
                return;
            }

            target.create(objectID, jdomContent, new Date(lastModified));
            withoutHistory.add(id);
        }
    }

    private void writeRevision(MCROCFLRevision step, MCRObjectID objectID) throws IOException {
        switch (step.type()) {
        case CREATED -> target.create(objectID, step.contentSupplier().get(), step.date());
        case MODIFIED -> target.update(objectID, step.contentSupplier().get(), step.date());
        case DELETED -> target.delete(objectID, step.date(), step.user());
        }
    }

    private MCROCFLRevision migrateRevision(MCRAbstractMetadataVersion<?> rev, MCRObjectID objectID)
        throws IOException {
        String user = rev.getUser();
        Date date = rev.getDate();
        LOGGER.info("Migrate revision {} of {}", rev.getRevision(), objectID);


        MCRMetadataVersionType type = MCRMetadataVersionType.fromValue(rev.getType());
        ContentSupplier supplier = type == MCRMetadataVersionType.DELETED ? null : () -> retriveActualContent(rev);
        return new MCROCFLRevision(type, supplier, user, date, objectID);
    }

    private MCRContent retriveActualContent(MCRAbstractMetadataVersion<?> rev) throws IOException {
        if (rev.getType() == MCRAbstractMetadataVersion.DELETED) {
            return null;
        }
        MCRContent content = rev.retrieve();
        Document document;

        try {
            document = content.asXML();
        } catch (JDOMException e) {
            throw new IOException("Error while reading as as XML", e);
        }
        return new MCRJDOMContent(document);
    }

    private List<? extends MCRAbstractMetadataVersion<?>> readRevisions(MCRObjectID objectID) {
        List<? extends MCRAbstractMetadataVersion<?>> revisions = null;
        MCRXMLMetadataManager instance = MCRXMLMetadataManager.instance();

        try {
            revisions = instance.listRevisions(objectID);
        } catch (Exception e) {
            LOGGER.error("Could not read revisions of {}", objectID, e);
        }
        return revisions;
    }

    public interface ContentSupplier {
        MCRContent get() throws IOException;
    }

}
