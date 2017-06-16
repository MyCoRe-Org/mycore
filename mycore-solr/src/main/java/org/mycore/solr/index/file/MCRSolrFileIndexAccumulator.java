package org.mycore.solr.index.file;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.solr.common.SolrInputDocument;

/**
 * This interface is used to accumulate information of a file to a solr document.
 * Add instances of this interface to the property <code>MCR.Module-solr.Indexer.File.AccumulatorList</code>
 */
public interface MCRSolrFileIndexAccumulator {
    /**
     * Adds additional information to a File.
     * @param document which holds the information
     * @param filePath to the file in a derivate
     * @param attributes of the file in a derivate
     */
    void accumulate(SolrInputDocument document, Path filePath, BasicFileAttributes attributes) throws IOException;
}
