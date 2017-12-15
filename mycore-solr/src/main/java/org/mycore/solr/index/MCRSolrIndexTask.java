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

package org.mycore.solr.index;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.SolrServerException;

/**
 * Solr index task which handles <code>MCRSolrIndexHandler</code>'s.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrIndexTask implements Callable<List<MCRSolrIndexHandler>> {

    protected MCRSolrIndexHandler indexHandler;

    /**
     * Creates a new solr index task.
     * 
     * @param indexHandler
     *            handles the index process
     */
    public MCRSolrIndexTask(MCRSolrIndexHandler indexHandler) {
        this.indexHandler = indexHandler;
    }

    @Override
    public List<MCRSolrIndexHandler> call() throws SolrServerException, IOException {
        long start = System.currentTimeMillis();
        this.indexHandler.index();
        long end = System.currentTimeMillis();
        indexHandler.getStatistic().addDocument(indexHandler.getDocuments());
        indexHandler.getStatistic().addTime(end - start);
        return this.indexHandler.getSubHandlers();
    }

    @Override
    public String toString() {
        return "Solr: " + this.indexHandler;
    }

}
