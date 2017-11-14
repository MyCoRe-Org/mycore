/*
 * $Id$
 * $Revision: 5697 $ $Date: Apr 11, 2013 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
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
