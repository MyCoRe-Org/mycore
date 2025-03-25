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
package org.mycore.solr.idmapper;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.idmapper.MCRDefaultIDMapper;
import org.mycore.frontend.idmapper.MCRIDMapper;
import org.mycore.solr.MCRSolrCoreManager;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;

/**
 * implementation of an MCRIDMapper
 *
 * It uses Solr to retrieve the real id from the given input string
 *
 * @author Robert Stephan
 */
public class MCRSolrIDMapper extends MCRDefaultIDMapper implements MCRIDMapper {

    private static final Logger LOGGER = LogManager.getLogger();

    private Set<String> objectSolrFields = Collections.emptySet();

    private Set<String> derivateSolrFields = Collections.emptySet();

    //property MCR.RestAPI.V2.AlternativeIdentifier.Objects.Keys deprecated in 2024.06
    @MCRProperty(name = "ObjectSolrFields", required = false,
        defaultName = "MCR.RestAPI.V2.AlternativeIdentifier.Objects.Keys")
    public void setObjectSolrFields(String fields) {
        objectSolrFields = Stream.ofNullable(fields).flatMap(MCRConfiguration2::splitValue).collect(Collectors.toSet());
    }

    //property MCR.RestAPI.V2.AlternativeIdentifier.Derivate.Keys deprecated in 2024.06
    @MCRProperty(name = "DerivateSolrFields", required = false,
        defaultName = "MCR.RestAPI.V2.AlternativeIdentifier.Derivates.Keys")
    public void setDerivateSolrFields(String fields) {
        derivateSolrFields =
            Stream.ofNullable(fields).flatMap(MCRConfiguration2::splitValue).collect(Collectors.toSet());
    }

    @Override
    public Optional<MCRObjectID> mapMCRObjectID(String mcrid) {
        String solrObjid = retrieveMCRObjectIDfromSOLR(mcrid);
        return super.mapMCRObjectID(solrObjid);
    }

    @Override
    public Optional<MCRObjectID> mapMCRDerivateID(MCRObjectID mcrObjId, String derid) {
        String solrDerid = retrieveMCRDerivateIDfromSOLR(mcrObjId, derid);
        return super.mapMCRDerivateID(mcrObjId, solrDerid);
    }

    /**
     * returns the input if the id syntax does not match and no solr keys are defined
     */
    private String retrieveMCRObjectIDfromSOLR(String mcrid) {
        String result = mcrid;
        if (mcrid != null && mcrid.contains(":") && !objectSolrFields.isEmpty()) {
            String key = mcrid.substring(0, mcrid.indexOf(':'));
            String value = mcrid.substring(mcrid.indexOf(':') + 1);
            if (objectSolrFields.contains(key)) {
                ModifiableSolrParams params = new ModifiableSolrParams();
                params.set("start", 0);
                params.set("rows", 1);
                params.set("fl", "id");
                params.set("fq", "objectKind:mycoreobject");
                params.set("q", key + ":" + ClientUtils.escapeQueryChars(value));
                QueryResponse solrResponse = null;
                try {
                    QueryRequest queryRequest = new QueryRequest(params);
                    MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(queryRequest,
                        MCRSolrAuthenticationLevel.SEARCH);
                    solrResponse = queryRequest.process(MCRSolrCoreManager.getMainSolrClient());
                } catch (Exception e) {
                    LOGGER.error("Error retrieving object id from SOLR", e);
                }
                if (solrResponse == null) {
                    return result;
                }
                SolrDocumentList solrResults = solrResponse.getResults();
                if (solrResults.getNumFound() == 1) {
                    result = String.valueOf(solrResults.get(0).getFieldValue("id"));
                }
                if (solrResults.getNumFound() == 0) {
                    LOGGER.info(() -> "ERROR: No MyCoRe ID found for query " + mcrid);
                }
                if (solrResults.getNumFound() > 1) {
                    LOGGER.info(() -> "ERROR: Multiple IDs found for query " + mcrid);
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
            && derid.contains(":") && !derivateSolrFields.isEmpty()) {
            String key = derid.substring(0, derid.indexOf(':'));
            String value = derid.substring(derid.indexOf(':') + 1);
            if (derivateSolrFields.contains(key)) {
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
                    QueryRequest queryRequest = new QueryRequest(params);
                    MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(queryRequest,
                        MCRSolrAuthenticationLevel.SEARCH);
                    solrResponse = queryRequest.process(MCRSolrCoreManager.getMainSolrClient());
                } catch (Exception e) {
                    LOGGER.error("Error retrieving derivate id from SOLR", e);
                }
                if (solrResponse == null) {
                    return result;
                }

                SolrDocumentList solrResults = solrResponse.getResults();
                if (solrResults.getNumFound() == 1) {
                    result = String.valueOf(solrResults.get(0).getFieldValue("id"));
                }
                if (solrResults.getNumFound() == 0) {
                    LOGGER.info(() -> "ERROR: No MyCoRe Derivate ID found for query " + derid);
                }
                if (solrResults.getNumFound() > 1) {
                    LOGGER.info(() -> "ERROR: Multiple IDs found for query " + derid);
                }
            }
        }
        return result;
    }

}
