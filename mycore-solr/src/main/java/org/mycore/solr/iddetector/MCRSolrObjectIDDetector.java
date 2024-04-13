package org.mycore.solr.iddetector;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.iddetector.MCRDefaultObjectIDetector;
import org.mycore.frontend.iddetector.MCRObjectIDDetector;
import org.mycore.solr.MCRSolrClientFactory;

public class MCRSolrObjectIDDetector extends MCRDefaultObjectIDetector implements MCRObjectIDDetector {

    private static final Logger LOGGER = LogManager.getLogger();

    private static Set<String> SEARCHKEYS_FOR_OBJECTS = MCRConfiguration2
        .getString("MCR.Solr.Object.IDDetector.Keys.Objects").or(
            // fallback (property key before 2024.06 LTS)
            () -> MCRConfiguration2
                .getString("MCR.RestAPI.V2.AlternativeIdentifier.Objects.Keys"))
        .stream()
        .flatMap(MCRConfiguration2::splitValue).collect(Collectors.toSet());

    private static Set<String> SEARCHKEYS_FOR_DERIVATES = MCRConfiguration2
        .getString("MCR.Solr.Object.IDDetector.Keys.Derivates").or(
            // fallback (property key before 2024.06 LTS)
            () -> MCRConfiguration2
                .getString("MCR.RestAPI.V2.AlternativeIdentifier.Derivates.Keys"))
        .stream()
        .flatMap(MCRConfiguration2::splitValue).collect(Collectors.toSet());

    @Override
    public Optional<MCRObjectID> detectMCRObjectID(String mcrid) {
        String solrObjid = retrieveMCRObjectIDfromSOLR(mcrid);
        return super.detectMCRObjectID(solrObjid);
    }

    @Override
    public Optional<MCRObjectID> detectMCRDerivateID(MCRObjectID mcrObjId, String derid) {
        String solrDerid = retrieveMCRDerivateIDfromSOLR(mcrObjId, derid);
        return super.detectMCRDerivateID(mcrObjId, solrDerid);
    }

    /**
     * returns the input if the id syntax does not match and no solr keys are defined
     */
    private String retrieveMCRObjectIDfromSOLR(String mcrid) {
        String result = mcrid;
        if (mcrid != null && mcrid.contains(":") && !SEARCHKEYS_FOR_OBJECTS.isEmpty()) {
            String key = mcrid.substring(0, mcrid.indexOf(":"));
            String value = mcrid.substring(mcrid.indexOf(":") + 1);
            if (SEARCHKEYS_FOR_OBJECTS.contains(key)) {
                ModifiableSolrParams params = new ModifiableSolrParams();
                params.set("start", 0);
                params.set("rows", 1);
                params.set("fl", "id");
                params.set("fq", "objectKind:mycoreobject");
                params.set("q", key + ":" + ClientUtils.escapeQueryChars(value));
                QueryResponse solrResponse = null;
                try {
                    solrResponse = MCRSolrClientFactory.getMainSolrClient().query(params);
                } catch (Exception e) {
                    LOGGER.error("Error retrieving object id from SOLR", e);
                }
                if (solrResponse != null) {
                    SolrDocumentList solrResults = solrResponse.getResults();
                    if (solrResults.getNumFound() == 1) {
                        result = String.valueOf(solrResults.get(0).getFieldValue("id"));
                    }
                    if (solrResults.getNumFound() == 0) {
                        LOGGER.info("ERROR: No MyCoRe ID found for query " + mcrid);
                    }
                    if (solrResults.getNumFound() > 1) {
                        LOGGER.info("ERROR: Multiple IDs found for query " + mcrid);
                    }
                }
            }
        }
        return result;
    }

    /**
     * returns the input if the id syntax does not match and no solr keys are defined
     */
    private String retrieveMCRDerivateIDfromSOLR(MCRObjectID mcrObjId, String derid) {
        String result = derid;
        if (mcrObjId != null && derid != null
            && derid.contains(":") && !SEARCHKEYS_FOR_DERIVATES.isEmpty()) {
            String key = derid.substring(0, derid.indexOf(":"));
            String value = derid.substring(derid.indexOf(":") + 1);
            if (SEARCHKEYS_FOR_DERIVATES.contains(key)) {
                ModifiableSolrParams params = new ModifiableSolrParams();
                params.set("start", 0);
                params.set("rows", 1);
                params.set("fl", "id");
                params.set("fq", "objectKind:mycorederivate");
                params.set("fq", "returnId:" + mcrObjId.toString());
                params.set("q", key + ":" + ClientUtils.escapeQueryChars(value));
                params.set("sort", "derivateOrder asc");
                QueryResponse solrResponse = null;
                try {
                    solrResponse = MCRSolrClientFactory.getMainSolrClient().query(params);
                } catch (Exception e) {
                    LOGGER.error("Error retrieving derivate id from SOLR", e);
                }
                if (solrResponse != null) {
                    SolrDocumentList solrResults = solrResponse.getResults();
                    if (solrResults.getNumFound() == 1) {
                        result = String.valueOf(solrResults.get(0).getFieldValue("id"));
                    }
                    if (solrResults.getNumFound() == 0) {
                        LOGGER.info("ERROR: No MyCoRe Derivate ID found for query " + derid);
                    }
                    if (solrResults.getNumFound() > 1) {
                        LOGGER.info("ERROR: Multiple IDs found for query " + derid);
                    }
                }
            }
        }
        return result;
    }

}
