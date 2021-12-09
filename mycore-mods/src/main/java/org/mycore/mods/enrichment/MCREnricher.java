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

package org.mycore.mods.enrichment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.mods.MCRMODSSorter;
import org.mycore.util.concurrent.MCRTransactionableCallable;

/**
 * Enriches a given MODS publication
 * by retrieving publication data from external data sources
 * and merging that data into the existing data.
 *
 * There may be different configurations for the enrichment process.
 * Each configuration has a unique ID and determines
 * the selection of data sources to query and the order and priority of merging.
 *
 * MCR.MODS.EnrichmentResolver.DataSources.[ConfigID]=[DataSourceID] [DataSourceID] [DataSourceID]...
 * e.g.
 * MCR.MODS.EnrichmentResolver.DataSources.import=(Scopus PubMed IEEE CrossRef DataCite) OADOI (LOBID GBV SWB) ZDB
 *
 * All data sources that support one of the identifiers present in the publication are queried.
 * A data source that successfully returned publication data will no longer be tried with one of other identifiers.
 * Should a data source return new, additional identifiers,
 * other data sources that support these identifiers will be tried again.
 *
 * The enrichment takes place on each publication level,
 * that means also the host (e.g. journal or book the article is published in)
 * and series level is enriched with external data.
 *
 * The order of the data source IDs in configuration determines the order of merging the publication data.
 * If some data sources are grouped using braces, merging is skipped after the first successul data source.
 * For example, when
 *
 * MCR.MODS.EnrichmentResolver.DataSources.sample=A (B C) D
 *
 * all four data sources are queried until all returned data or all identifiers habe been tried.
 * Afterwards, the data returned from A is merged into the original data.
 * Next, if B returned data, the data of B is merged and the data of C will be ignored.
 * If B did not return data and C returned data, the data of C is merged.
 * At the end, the data of D is merged.
 * So building groups of data sources with braces can be used to express data source priority.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCREnricher {

    private static final Logger LOGGER = LogManager.getLogger(MCREnricher.class);

    private static final String XPATH_HOST_SERIES = "mods:relatedItem[@type='host' or @type='series']";

    private XPathExpression<Element> xPath2FindNestedObjects;

    private String dsConfig;

    Element publication;

    MCRIdentifierPool idPool;

    Map<String, MCRDataSourceCall> id2call = new HashMap<>();

    MCREnricher(String configID) {
        xPath2FindNestedObjects = XPathFactory.instance().compile(XPATH_HOST_SERIES, Filters.element(), null,
            MCRConstants.getStandardNamespaces());

        dsConfig = MCRConfiguration2.getStringOrThrow("MCR.MODS.EnrichmentResolver.DataSources." + configID);

        idPool = new MCRIdentifierPool();
        prepareDataSourceCalls(dsConfig);
    }

    public void enrich(Element publication) {
        this.publication = publication;
        idPool.addIdentifiersFrom(publication);

        resolveExternalData();
        mergeExternalData();
        MCRMODSSorter.sort(publication);

        for (Element nestedObject : xPath2FindNestedObjects.evaluate(publication)) {
            idPool.continueWithNewIdentifiers();
            id2call.values().forEach(call -> call.reset());
            enrich(nestedObject);
        }
    }

    private void prepareDataSourceCalls(String dsConfig) {
        for (StringTokenizer st = new StringTokenizer(dsConfig, " ()", false); st.hasMoreTokens();) {
            String dataSourceID = st.nextToken();
            MCRDataSource dataSource = MCRDataSourceFactory.instance().getDataSource(dataSourceID);
            MCRDataSourceCall call = new MCRDataSourceCall(dataSource, idPool);
            id2call.put(dataSourceID, call);
        }
    }

    private void resolveExternalData() {
        Collection<MCRTransactionableCallable<Boolean>> calls = id2call.values()
            .stream()
            .map(MCRTransactionableCallable::new)
            .collect(Collectors.toList());
        ExecutorService executor = Executors.newFixedThreadPool(calls.size());
        try {
            while (idPool.hasNewIdentifiers()) {
                idPool.buildNewIdentifiersIn(publication);
                idPool.continueWithNewIdentifiers();
                executor.invokeAll(calls);
            }
        } catch (InterruptedException ex) {
            LOGGER.warn(ex);
        } finally {
            executor.shutdown();
        }
    }

    private void mergeExternalData() {
        boolean withinGroup = false;
        String delimiters = " ()";

        for (StringTokenizer st = new StringTokenizer(dsConfig, delimiters, true); st.hasMoreTokens();) {
            String token = st.nextToken(delimiters).trim();
            if (token.isEmpty()) {
                continue;
            } else if ("(".equals(token)) {
                withinGroup = true;
            } else if (")".equals(token)) {
                withinGroup = false;
            } else {
                MCRDataSourceCall call = id2call.get(token);
                if (call.wasSuccessful()) {
                    LOGGER.info("merging data from " + token);
                    call.mergeResultWith(publication);
                    if (withinGroup) {
                        st.nextToken(")"); // skip forward to end of group
                        withinGroup = false;
                    }
                }
            }
        }
    }
}
