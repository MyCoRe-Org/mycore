/**
 * 
 */
package org.mycore.solr.utils;


import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.mycore.common.MCRConfiguration;
import org.mycore.solr.MCRSolrServerFactory;

/**
 * @author shermann
 * 
 */
public class MCRSolrUtils {

	public static final String CONFIG_PREFIX = "MCR.Module-solr.";

	private static Logger LOGGER = Logger.getLogger(MCRSolrUtils.class);

	/**
	 * Get the number of hits for a given solr query.
	 * 
	 * @param solrQuery
	 *            the solr query to execute
	 * 
	 * @return the number of hits or 0 in case there are 0 hits or an error
	 *         occured
	 */
	public static long getNumHits(String solrQuery) {
		SolrQuery q = new SolrQuery(solrQuery);
		q.setRows(0);
		SolrServer solrServer = MCRSolrServerFactory.getSolrServer();
		long numFound = 0;
		try {
			QueryResponse queryResponse = solrServer.query(q);
			numFound = queryResponse.getResults().getNumFound();
		} catch (SolrServerException e) {
			LOGGER.error("Could not execute query", e);
		}
		return numFound;
	}

	/**
	 * @param query
	 * @param rows
	 * @return
	 * @throws SolrServerException
	 */
	public static SolrDocumentList getQueryResults(String query, int rows)
			throws SolrServerException {
		SolrQuery q = new SolrQuery(query);
		q.setRows(rows);
		return MCRSolrServerFactory.getSolrServer().query(q).getResults();
	}

	/**
	 * Gets a property for the solr module
	 * 
	 * @param propertyName
	 *            the name of the property
	 * @return the value || null
	 */
	public static String getSolrPropertyValue(String propertyName) {
		return getSolrPropertyValue(propertyName, null);
	}

	/**
	 * Gets a property for the solr module
	 * 
	 * @param propertyName
	 *            the name of the property
	 * @param defaultValue
	 *            a default value if the property is not set
	 * @return the value
	 */
	public static String getSolrPropertyValue(String propertyName,
			String defaultValue) {
		return MCRConfiguration.instance().getString(
				CONFIG_PREFIX + propertyName, defaultValue);
	}
}
