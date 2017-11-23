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

package org.mycore.solr.index.handlers;

import java.io.IOException;
import java.text.MessageFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;
import org.mycore.solr.index.statistic.MCRSolrIndexStatisticCollector;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrOptimizeIndexHandler extends MCRSolrAbstractIndexHandler {
    private static final Logger LOGGER = LogManager.getLogger(MCRSolrOptimizeIndexHandler.class);

    @Override
    public void index() throws IOException, SolrServerException {
        LOGGER.info("Sending optimize request to solr");
        UpdateResponse response = getSolrClient().optimize();
        LOGGER.info(MessageFormat.format("Optimize was {0}({1}ms)",
            (response.getStatus() == 0 ? "successful." : "UNSUCCESSFUL!"), response.getElapsedTime()));
    }

    @Override
    public MCRSolrIndexStatistic getStatistic() {
        return MCRSolrIndexStatisticCollector.OPERATIONS;
    }

    @Override
    public String toString() {
        return "optimize index";
    }

}
