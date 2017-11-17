package org.mycore.solr.index.handlers.stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.index.cs.MCRSolrAbstractContentStream;
import org.mycore.solr.index.cs.MCRSolrContentStream;

import java.io.IOException;
import java.util.Objects;

/**
 * Index one mycore object with a MCRSolrContentStream.
 *
 * @author Matthias Eichner
 */
public class MCRSolrSingleObjectStreamIndexHandler extends MCRSolrObjectStreamIndexHandler {

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrSingleObjectStreamIndexHandler.class);

    protected MCRObjectID id;

    protected MCRContent content;

    public MCRSolrSingleObjectStreamIndexHandler(MCRObjectID id, MCRContent content) {
        this(id, content, null);
    }

    public MCRSolrSingleObjectStreamIndexHandler(MCRObjectID id, MCRContent content, SolrClient solrClient) {
        super(solrClient);
        this.id = Objects.requireNonNull(id);
        this.content = Objects.requireNonNull(content);
    }

    @Override
    protected MCRSolrAbstractContentStream<?> getStream() {
        return new MCRSolrContentStream(id.toString(), content);
    }

    /**
     * Invokes an index request for the current content stream.
     */
    public void index() throws IOException, SolrServerException {
        if(!MCRMetadataManager.exists(id)) {
            LOGGER.warn("Unable to index '{}' cause it doesn't exists anymore!", id);
            return;
        }
        super.index();
    }

    @Override
    public String toString() {
        return "index " + id;
    }

}
