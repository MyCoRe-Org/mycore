/*
 * $Revision$ $Date$
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

package org.mycore.services.fieldquery;

import java.io.StringReader;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.mycore.common.MCRConfiguration;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearcher;
import org.mycore.services.fieldquery.MCRSearcherFactory;

/**
 * Uses package queries from lucene/contrib/queries to do a "more like this"
 * query. When using this class, you have to define a field "score" in
 * searchfields.xml:
 * 
 * <field name="score" type="decimal" source="searcherHitMetadata" />
 * 
 * @author Harald Richter
 * @author Frank Lützenkirchen
 */
public class MCRMoreLikeThis {

    /** The Log4J Logger * */
    private final static Logger LOGGER = Logger.getLogger(MCRMoreLikeThis.class);

    /**
     * Does "more like this" query, used to find documents with similar content.
     * The results contain a metadata field "score" which contains a value
     * between 0 and 1 indicating similarity to the given field value.
     * 
     * @param value
     *            the MCRFieldValue to search similar entries for
     * @param minScore
     *            the minimum score the hits must reach, a value between 0 and
     *            1, for example 0.9
     * @param maxResults
     *            the maximum number of hits to return. A value less than 1
     *            means to return all hits.
     * 
     * @return the MCRResults of the search
     * 
     */
    public static MCRResults moreLikeThis(MCRFieldValue value, double minScore, int maxResults) throws Exception {
        minScore = Math.max(minScore, 1.0);
        minScore = Math.min(minScore, 0.0);

        if (maxResults < 1)
            maxResults = Integer.MAX_VALUE;

        String index = value.getField().getIndex();
        MCRSearcher searcher = MCRSearcherFactory.getSearcherForIndex(index);
        String property = "MCR.Searcher." + searcher.getID() + ".IndexDir";
        String indexDir = MCRConfiguration.instance().getString(property);

        IndexReader ir = IndexReader.open(indexDir);
        MoreLikeThis mlt = new MoreLikeThis(ir);
        mlt.setAnalyzer(new GermanAnalyzer()); // TODO: Make this configurable

        // Ignore terms with less than this frequency in the source doc.
        mlt.setMinTermFreq(1); // default 2

        // Ignore words which do not occur in at least this many docs.
        mlt.setMinDocFreq(2); // default 5

        // Ignore words less than this length or if 0 then this has no effect.
        mlt.setMinWordLen(6); // default 5

        mlt.setFieldNames(new String[] { value.getField().getName() });

        IndexSearcher is = new IndexSearcher(indexDir);
        Query query = mlt.like(new StringReader(value.getValue()));
        LOGGER.info("MCRMoreLikeThis searching " + query);

        Hits hits = is.search(query);
        int len = hits.length();
        LOGGER.info("MCRMoreLikeThis found " + len + " entries matching");

        MCRResults results = new MCRResults();
        for (int i = 0; (i < len) && (results.getNumHits() >= maxResults); i++) {
            Document doc = hits.doc(i);
            float score = hits.score(i);
            String hitID = doc.get("returnid");

            if (score >= minScore) {
                LOGGER.debug("MCRMoreLikeThis score for " + hitID + " = " + score);
                MCRHit hit = new MCRHit(hitID);
                hit.addMetaData(new MCRFieldValue(MCRFieldDef.getDef("score"), String.valueOf(score)));
                results.addHit(hit);
            }
        }

        return results;
    }
}
