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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.solr.common.SolrInputDocument;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;
import org.mycore.solr.index.statistic.MCRSolrIndexStatisticCollector;

/**
 * Abstract base class for handlers that deal with indexing errors.
 * <p>
 * Provides a common utility method to construct a basic Solr document containing
 * essential error information like the message and stack trace.
 *
 * @author Matthias Eichner
 */
public abstract class MCRSolrAbstractErrorIndexHandler extends MCRSolrAbstractIndexHandler {

    /**
     * Builds a standard {@link SolrInputDocument} for an indexing error.
     * <p>
     * The document will contain the following fields:
     * <ul>
     *   <li><b>id</b>: The ID of the document that failed to index.</li>
     *   <li><b>error_message</b>: The message from the causative exception.</li>
     *   <li><b>error_stacktrace</b>: The full stack trace of the causative exception.</li>
     * </ul>
     *
     * @param id The ID of the failed document.
     * @param cause The exception that caused the failure.
     * @return A SolrInputDocument populated with error details.
     */
    protected SolrInputDocument buildDocument(String id, Exception cause) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        cause.printStackTrace(pw);

        SolrInputDocument errorDoc = new SolrInputDocument();
        errorDoc.setField("id", id);
        errorDoc.setField("error_message", cause.getMessage());
        errorDoc.setField("error_stacktrace", sw.toString());
        return errorDoc;
    }

    @Override
    public MCRSolrIndexStatistic getStatistic() {
        return MCRSolrIndexStatisticCollector.DOCUMENTS;
    }

}
