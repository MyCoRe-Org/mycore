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
import org.mycore.mods.merger.MCRMergeTool;
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
 * 
 * Depending on the configuration properties  
 * MCR.MODS.EnrichmentResolver.DefaultStopOnFirstResult=true|false
 * and
 * MCR.MODS.EnrichmentResolver.DataSource.[ID].StopOnFirstResult=true|false
 * the data source will stop trying to resolve publication data after 
 * the first successful call with a given identifier and skip others, 
 * or will try to retrieve data for all given identifiers.
 * 
 * Should a data source return new, additional identifiers,
 * other data sources that support these identifiers will be tried again.
 *
 * The enrichment takes place on each publication level,
 * that means also the host (e.g. journal or book the article is published in)
 * and series level is enriched with external data.
 *
 * The order of the data source IDs in configuration determines the order of merging the publication data.
 * If some data sources are grouped using braces, merging is skipped after the first successful data source.
 * For example, when
 *
 * MCR.MODS.EnrichmentResolver.DataSources.sample=A (B C) D
 *
 * all four data sources are queried until all returned data or all identifiers have been tried.
 * Afterwards, the data returned from A is merged into the original data.
 * Next, if B returned data, the data of B is merged and the data of C will be ignored.
 * If B did not return data and C returned data, the data of C is merged.
 * At the end, the data of D is merged.
 * So building groups of data sources with braces can be used to express data source priority.
 *
 * @see MCRDataSource
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCREnricher {

    private static final Logger LOGGER = LogManager.getLogger(MCREnricher.class);

    private static final String XPATH_HOST_SERIES = "mods:relatedItem[@type='host' or @type='series']";

    private static final String DELIMITERS = " ()";

    private XPathExpression<Element> xPath2FindNestedObjects;

    private String dsConfig;

    private MCRIdentifierPool idPool;

    private Map<String, MCRDataSourceCall> id2call;

    private MCREnrichmentDebugger debugger = new MCRNoOpEnrichmentDebugger();

    public MCREnricher(String configID) {
        xPath2FindNestedObjects = XPathFactory.instance().compile(XPATH_HOST_SERIES, Filters.element(), null,
            MCRConstants.getStandardNamespaces());

        dsConfig = MCRConfiguration2.getStringOrThrow("MCR.MODS.EnrichmentResolver.DataSources." + configID);
    }

    public void setDebugger(MCREnrichmentDebugger debugger) {
        this.debugger = debugger;
    }

    public synchronized void enrich(Element publication) {
        debugger.debugPublication("before", publication);
        
        idPool = new MCRIdentifierPool();
        id2call = prepareDataSourceCalls();

        idPool.addIdentifiersFrom(publication);
        while (idPool.hasNewIdentifiers()) {
            debugger.startIteration();
            debugger.debugNewIdentifiers(idPool.getNewIdentifiers());

            idPool.prepareNextIteration();

            resolveExternalData();
            mergeNewIdentifiers(publication);
            mergeExternalData(publication);
            MCRMODSSorter.sort(publication);

            debugger.endIteration();
        }

        for (Element nestedObject : xPath2FindNestedObjects.evaluate(publication)) {
            enrich(nestedObject);
        }
    }

    private void mergeNewIdentifiers(Element publication) {
        idPool.getNewIdentifiers().forEach(id -> id.mergeInto(publication));
    }

    private Map<String, MCRDataSourceCall> prepareDataSourceCalls() {
        id2call = new HashMap<>();
        for (StringTokenizer st = new StringTokenizer(dsConfig, DELIMITERS, false); st.hasMoreTokens();) {
            String dataSourceID = st.nextToken();
            MCRDataSource dataSource = MCRDataSourceFactory.instance().getDataSource(dataSourceID);
            MCRDataSourceCall call = new MCRDataSourceCall(dataSource, idPool);
            id2call.put(dataSourceID, call);
        }
        return id2call;
    }

    private void resolveExternalData() {
        Collection<MCRTransactionableCallable<Boolean>> calls = id2call.values()
            .stream()
            .map(MCRTransactionableCallable::new)
            .collect(Collectors.toList());
        ExecutorService executor = Executors.newFixedThreadPool(calls.size());
        try {
            executor.invokeAll(calls);
        } catch (InterruptedException ex) {
            LOGGER.warn(ex);
        } finally {
            executor.shutdown();
        }
    }

    private void mergeExternalData(Element publication) {
        boolean withinGroup = false;

        for (StringTokenizer st = new StringTokenizer(dsConfig, DELIMITERS, true); st.hasMoreTokens();) {
            String token = st.nextToken(DELIMITERS).trim();
            if (token.isEmpty()) {
                continue;
            } else if ("(".equals(token)) {
                withinGroup = true;
            } else if (")".equals(token)) {
                withinGroup = false;
            } else {
                MCRDataSourceCall call = id2call.get(token);
                if (call.wasSuccessful()) {
                    call.getResults().forEach(result -> {
                        LOGGER.info("merging data from " + token);
                        merge(publication, result);
                        debugger.debugResolved(token, result);
                        debugger.debugPublication("afterMerge", publication);
                    });
                    call.clearResults();

                    if (withinGroup) {
                        st.nextToken(")"); // skip forward to end of group
                        withinGroup = false;
                    }
                }
            }
        }
    }

    static void merge(Element publication, Element toMergeWith) {
        if (publication.getName().equals("relatedItem")) {
            // resolved is always mods:mods, transform to mods:relatedItem to be mergeable
            toMergeWith.setName("relatedItem");
            toMergeWith.setAttribute(publication.getAttribute("type").clone());
        }

        MCRMergeTool.merge(publication, toMergeWith);
    }
}
