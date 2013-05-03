package org.mycore.solr.index.handlers.stream;

import static org.mycore.solr.MCRSolrConstants.EXTRACT_PATH;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.solr.index.cs.MCRSolrFileContentStream;
import org.mycore.solr.index.file.MCRSolrMCRFileDocumentFactory;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;
import org.mycore.solr.index.statistic.MCRSolrIndexStatisticCollector;

public class MCRSolrFileIndexHandler extends MCRSolrAbstractStreamIndexHandler {

    final static Logger LOGGER = Logger.getLogger(MCRSolrFileIndexHandler.class);

    final static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");

    public MCRSolrFileIndexHandler(MCRSolrFileContentStream stream) {
        super(stream);
    }

    public MCRSolrFileIndexHandler(MCRSolrFileContentStream stream, SolrServer solrServer) {
        super(stream, solrServer);
    }

    @Override
    public void index() throws SolrServerException, IOException {
        MCRFile file = getStream().getSource();
        String solrID = file.getID();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Solr: indexing file \"" + file.getAbsolutePath() + " (" + solrID + ")\"");
        }
        /* create the update request object */
        ContentStreamUpdateRequest updateRequest = new ContentStreamUpdateRequest(EXTRACT_PATH);
        MCRSolrFileContentStream fileContentStream = getStream();
        updateRequest.addContentStream(fileContentStream);

        /* set the additional parameters */
        updateRequest.setParams(getSolrParams(file));
        updateRequest.setCommitWithin(getCommitWithin());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Solr: sending binary data (" + file.getAbsolutePath() + " (" + solrID + "), size is " + file.getSizeFormatted()
                + ") to solr server.");
        }
        long t = System.currentTimeMillis();
        /* actually send the request */
        getSolrServer().request(updateRequest);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Solr: sending binary data \"" + file.getAbsolutePath() + " (" + solrID + ")\"" + " done in "
                + (System.currentTimeMillis() - t) + "ms");
        }
    }

    private ModifiableSolrParams getSolrParams(MCRFile file) throws IOException {
        ModifiableSolrParams params = new ModifiableSolrParams();
        SolrInputDocument doc = MCRSolrMCRFileDocumentFactory.getInstance().getDocument(file);
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
    public MCRSolrFileContentStream getStream() {
        return (MCRSolrFileContentStream) super.getStream();
    }

    @Override
    public MCRSolrIndexStatistic getStatistic() {
        return MCRSolrIndexStatisticCollector.fileTransfer;
    }

    @Override
    public int getDocuments() {
        return 1;
    }

}
