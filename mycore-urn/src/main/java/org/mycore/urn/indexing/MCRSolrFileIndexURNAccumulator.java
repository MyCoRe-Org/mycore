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
