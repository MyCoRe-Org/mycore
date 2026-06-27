package org.mycore.dedup;

import static org.mycore.solr.MCRSolrCoreManager.getMainSolrClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Document;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;

/**
 * Service class responsible for querying the Solr index to detect potential duplicate MCR objects
 * based on specified deduplication criteria.
 * <p>
 * This class encapsulates Solr search logic, authentication, and response parsing,
 * returning JDOM {@link org.jdom2.Document} objects for further processing.
 * </p>
 */

public class MCRSolrDeduplicationService {

    private final SolrClient solrClient;

    public MCRSolrDeduplicationService() {
        this.solrClient = getMainSolrClient();
    }

    /**
     * Executes a Solr search using the given query string and returns all matching
     * MCR objects as JDOM {@link org.jdom2.Document}s.
     *
     * @param query the Solr query string
     * @return a list of {@link org.jdom2.Document} objects corresponding to matching Solr documents
     * @throws SolrServerException if Solr is unavailable or returns an error
     * @throws IOException         if communication with the Solr server fails
     */
    public List<Document> checkForDuplicates(String query,String selfID)
            throws SolrServerException, IOException {
        SolrQuery solrQuery = buildSolrQuery(query);
        QueryResponse response = executeQuery(solrQuery);
        return  parseResponse(response,selfID);
    }


    /**
     * Builds a basic {@link SolrQuery} object from a query string.
     * Limits results to 10 documents starting from index 0.
     *
     * @param query the raw Solr query string
     * @return a configured {@link SolrQuery} object
     */
    private SolrQuery buildSolrQuery(String query) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setRows(10);
        solrQuery.setStart(0);
        return solrQuery;
    }

    /**
     * Executes a Solr query with proper MCR authentication applied.
     *
     * @param solrQuery the query to execute
     * @return the {@link QueryResponse} returned by Solr
     * @throws SolrServerException if Solr returns an error
     * @throws IOException         if the query fails due to IO issues
     */
    private QueryResponse executeQuery(SolrQuery solrQuery)
            throws SolrServerException, IOException {
        QueryRequest request = new QueryRequest(solrQuery);
        MCRSolrAuthenticationManager.obtainInstance()
                .applyAuthentication(request, MCRSolrAuthenticationLevel.SEARCH);
        return request.process(solrClient);
    }

    /**
     * Parses the Solr response and retrieves the corresponding MCR objects as JDOM documents.
     *
     * @param response the response returned by Solr
     * @return a list of {@link org.jdom2.Document} objects representing the found MCR objects
     */
    private List<Document> parseResponse(QueryResponse response, String selfId) {
        List<Document> documents = new ArrayList<>();
        for (SolrDocument doc : response.getResults()) {
            String docId = String.valueOf(doc.getFieldValue("id"));
            if (!Objects.equals(docId, selfId)) {
            MCRObjectID id = MCRObjectID.getInstance(docId);
            MCRObject obj = MCRMetadataManager.retrieveMCRObject(id);
            documents.add(obj.createXML());
            }
        }
        return documents;
    }
}

