package org.mycore.solr.index.handlers.stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.common.MCRUtils;
import org.mycore.solr.index.cs.MCRSolrPathContentStream;
import org.mycore.solr.index.file.MCRSolrPathDocumentFactory;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;
import org.mycore.solr.index.statistic.MCRSolrIndexStatisticCollector;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;

import static org.mycore.solr.MCRSolrConstants.EXTRACT_PATH;

public class MCRSolrFileIndexHandler extends MCRSolrAbstractStreamIndexHandler {

    final static Logger LOGGER = LogManager.getLogger(MCRSolrFileIndexHandler.class);

    protected Path file;

    protected BasicFileAttributes attrs;

    public MCRSolrFileIndexHandler(Path file, BasicFileAttributes attrs, SolrClient solrClient) {
        super(solrClient);
        this.file = file;
        this.attrs = attrs;
    }

    public MCRSolrPathContentStream getStream() {
        return new MCRSolrPathContentStream(file, attrs);
    }

    @Override
    public void index() throws SolrServerException, IOException {
        String solrID = file.toUri().toString();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Solr: indexing file \"" + file.toString() + "\"");
        }
        /* create the update request object */
        ContentStreamUpdateRequest updateRequest = new ContentStreamUpdateRequest(EXTRACT_PATH);
        updateRequest.addContentStream(getStream());

        /* set the additional parameters */
        updateRequest.setParams(getSolrParams(file, attrs));
        updateRequest.setCommitWithin(getCommitWithin());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Solr: sending binary data (" + file.toString() + " (" + solrID + "), size is " + MCRUtils
                .getSizeFormatted(attrs.size()) + ") to solr server.");
        }
        long t = System.currentTimeMillis();
        /* actually send the request */
        getSolrClient().request(updateRequest);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Solr: sending binary data \"" + file.toString() + " (" + solrID + ")\"" + " done in " + (
                System.currentTimeMillis() - t) + "ms");
        }
    }

    private ModifiableSolrParams getSolrParams(Path file, BasicFileAttributes attrs) throws IOException {
        ModifiableSolrParams params = new ModifiableSolrParams();
        SolrInputDocument doc = MCRSolrPathDocumentFactory.getInstance().getDocument(file, attrs);
        for (SolrInputField field : doc) {
            String name = "literal." + field.getName();
            if (field.getValueCount() > 1) {
                String[] values = getValues(field.getValues());
                params.set(name, values);
            } else {
                params.set(name, field.getValue().toString());
            }
        }
        return params;
    }

    private String[] getValues(Collection<Object> values) {
        ArrayList<String> strValues = new ArrayList<>(values.size());
        for (Object o : values) {
            strValues.add(o.toString());
        }
        return strValues.toArray(new String[strValues.size()]);
    }

    @Override
    public MCRSolrIndexStatistic getStatistic() {
        return MCRSolrIndexStatisticCollector.fileTransfer;
    }

    @Override
    public int getDocuments() {
        return 1;
    }

    @Override
    public String toString() {
        return "index " + this.file.toString();
    }

}
