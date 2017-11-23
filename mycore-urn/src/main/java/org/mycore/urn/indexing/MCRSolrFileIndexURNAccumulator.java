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

package org.mycore.urn.indexing;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.solr.common.SolrInputDocument;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.solr.index.file.MCRSolrFileIndexAccumulator;
import org.mycore.urn.services.MCRURNManager;

/**
 * Adds URNS to SOLR Index
 * Changeable in property <code>MCR.Module-solr.Indexer.File.AccumulatorList</code>
 */
@Deprecated
public class MCRSolrFileIndexURNAccumulator implements MCRSolrFileIndexAccumulator {
    @Override
    public void accumulate(SolrInputDocument document, Path filePath, BasicFileAttributes attributes)
        throws IOException {
        MCRPath mcrPath = MCRPath.toMCRPath(filePath); //check if this is an MCRPath -> more metadata
        String ownerID = mcrPath.getOwner();
        String absolutePath = '/' + filePath.subpath(0, filePath.getNameCount()).toString();

        String urn = MCRURNManager.getURNForFile(ownerID,
            absolutePath.substring(0, absolutePath.lastIndexOf("/") + 1), filePath.getFileName().toString());
        if (urn != null) {
            document.setField("fileURN", urn);
        }
    }
}
