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

package org.mycore.solr.index.handlers.stream;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;
import static org.mycore.solr.MCRSolrConstants.UPDATE_PATH;

import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.util.NamedList;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;
import org.mycore.solr.index.statistic.MCRSolrIndexStatisticCollector;

/**
 * Base class for stream indexing MCRObject's with the default UPDATE_PATH.
 *
 * @author Matthias Eichner
 */
public abstract class MCRSolrObjectStreamIndexHandler extends MCRSolrAbstractStreamIndexHandler {

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrObjectStreamIndexHandler.class);

    static final String STYLESHEET = MCRConfiguration.instance()
        .getString(CONFIG_PREFIX + "IndexHandler.ContentStream.ServerStyleSheet");

    public MCRSolrObjectStreamIndexHandler() {
        this(null);
    }

    public MCRSolrObjectStreamIndexHandler(SolrClient solrClient) {
        super(solrClient);
    }

    public void index() throws IOException, SolrServerException {
        long tStart = System.currentTimeMillis();
        ContentStreamUpdateRequest updateRequest = new ContentStreamUpdateRequest(UPDATE_PATH);
        updateRequest.addContentStream(getStream());
        if (STYLESHEET.length() > 0) {
            updateRequest.setParam("tr", STYLESHEET);
        }
        updateRequest.setCommitWithin(getCommitWithin());
        NamedList<Object> request = getSolrClient().request(updateRequest);
        if (LOGGER.isDebugEnabled()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Solr: indexing data of \"");
            stringBuilder.append(this);
            stringBuilder.append("\" (");
            stringBuilder.append((System.currentTimeMillis() - tStart));
            stringBuilder.append("ms)");
            for (Map.Entry<String, Object> entry : request) {
                stringBuilder.append('\n');
                stringBuilder.append(entry.getKey());
                stringBuilder.append('=');
                stringBuilder.append(entry.getValue());
            }
            LOGGER.debug(stringBuilder.toString());
        }
    }

    @Override
    public MCRSolrIndexStatistic getStatistic() {
        return MCRSolrIndexStatisticCollector.XML;
    }

}
