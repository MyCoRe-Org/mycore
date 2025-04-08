/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRMetadataVersionType;

/**
 * A pruner that combines and compares revisions to decide which to keep and which to discard.
 * The method {@link #getMergeDecider()} should return a {@link RevisionMergeDecider} that decides if two
 * revisions should be merged.
 * <p>
 * The method {@link #buildMergedRevision(MCROCFLRevision, MCROCFLRevision, Document, Document)} should return a new
 * revision that is the result of merging the two given revisions.
 */
public abstract class MCROCFLCombineComparePruner implements MCROCFLRevisionPruner {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public List<MCROCFLRevision> prune(List<MCROCFLRevision> revisions) throws IOException, JDOMException {
        List<MCROCFLRevision> newRevisions = new ArrayList<>();
        Iterator<MCROCFLRevision> iterator = revisions.listIterator();

        if (!iterator.hasNext()) {
            return newRevisions;
        }

        MCROCFLRevision last = iterator.next();
        RevisionMergeDecider comparator = getMergeDecider();

        while (iterator.hasNext()) {
            MCROCFLRevision next = iterator.next();

            if (last.type() != MCRMetadataVersionType.DELETED && next.type() != MCRMetadataVersionType.DELETED) {
                MCRContent currentContent = last.contentSupplier().get();
                Document currentDocument = currentContent.asXML();

                MCRContent nextContent = next.contentSupplier().get();
                Document nextDocument = nextContent.asXML();

                if (comparator.shouldMerge(last, currentDocument, next, nextDocument)) {
                    LOGGER.info("Merging revisions {} and {}", last, next);
                    last = buildMergedRevision(last, next, currentDocument, nextDocument);
                    continue;
                }
            }

            LOGGER.info("Keeping revision {}", last);
            newRevisions.add(last);
            last = next;
        }

        LOGGER.info("Keeping revision {}", last);
        newRevisions.add(last);

        return newRevisions;
    }

    /**
     * Returns a {@link RevisionMergeDecider} that decides if two revisions should be merged.
     * @return a {@link RevisionMergeDecider}
     */
    public abstract RevisionMergeDecider getMergeDecider();

    /**
     * Builds a new revision that is the result of merging the two given revisions.
     * @param current the current revision
     * @param next the next revision to merge
     * @param currentDocument the current document
     * @param nextDocument the document of the next revision
     * @return a new revision that is the result of merging the two given revisions
     */
    public abstract MCROCFLRevision buildMergedRevision(MCROCFLRevision current, MCROCFLRevision next,
        Document currentDocument, Document nextDocument);

    /**
     * A decider that decides if two revisions should be merged.
     */
    public interface RevisionMergeDecider {
        boolean shouldMerge(MCROCFLRevision revision1, Document document1,
            MCROCFLRevision revision2, Document document2);
    }

}
