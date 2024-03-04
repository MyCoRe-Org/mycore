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
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;

public abstract class MCROCFLCombineComparePruner implements MCROCFLRevisionPruner {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public List<MCROCFLRevision> prune(List<MCROCFLRevision> revisions) throws IOException, JDOMException {
        ArrayList<MCROCFLRevision> newRevisions = new ArrayList<>();
        Iterator<MCROCFLRevision> iterator = revisions.listIterator();

        if (!iterator.hasNext()) {
            return newRevisions;
        }

        MCROCFLRevision last = iterator.next();
        RevisionMergeDecider comparator = getMergeDecider();

        while (iterator.hasNext()) {
            MCROCFLRevision next = iterator.next();

            if ((last.getType() == MCROCFLVersionType.CREATE || last.getType() == MCROCFLVersionType.UPDATE) &&
                    (next.getType() == MCROCFLVersionType.CREATE || next.getType() == MCROCFLVersionType.UPDATE)) {
                MCRContent currentContent = last.getContentSupplier().get();
                Document currentDocument = currentContent.asXML();

                MCRContent nextContent = next.getContentSupplier().get();
                Document nextDocument = nextContent.asXML();

                if (comparator.shouldMerge(last, currentDocument, next,nextDocument)) {
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

    public abstract RevisionMergeDecider getMergeDecider();

    public abstract MCROCFLRevision buildMergedRevision(MCROCFLRevision current, MCROCFLRevision next,
        Document currentDocument, Document nextDocument);

    public interface RevisionMergeDecider {

        boolean shouldMerge(MCROCFLRevision r1, Document o1, MCROCFLRevision r2, Document o2);
    }

}

