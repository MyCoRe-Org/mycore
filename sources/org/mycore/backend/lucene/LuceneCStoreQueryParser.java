/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.backend.lucene;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

/**
 * This class provides methods to search derivates on a lucene index
 * 
 * @author Thomas Scheffler (yagee)
 */
public class LuceneCStoreQueryParser extends QueryParser {
    private static final Logger logger = Logger
            .getLogger(MCRCStoreLucene.class);

    protected static final String GROUPING_FIELD = "DerivateID";

    protected static final LuceneCStoreQueryParser WS_PARSER = new LuceneCStoreQueryParser(
            null, new WhitespaceAnalyzer());

    private String groupingValue;

    Analyzer analyzer;

    String field;

    /**
     * uses org.apache.lucene.queryParser.QueryParser to parse a query and
     * delivers a query that handles special condition of mycore derivates
     * 
     * @param f
     *            the default field for query terms.
     * @param a
     *            used to find terms in the query text.
     */
    public LuceneCStoreQueryParser(String f, Analyzer a) {
        super(f, a);
        analyzer = a;
        field = f;
    }

    /**
     * @return
     */
    public String getGroupingValue() {
        return groupingValue;
    }

    /**
     * Must be set prior parsing a query to the DerivateID to be searched on
     * 
     * @param string
     *            DerivateID
     */
    public void setGroupingValue(String string) {
        if (string.indexOf(' ') != -1)
            logger.error("Grouping value may not contain space characters");
        groupingValue = string;
        logger.debug(new StringBuffer("Set ").append(GROUPING_FIELD).append(
                " to ").append(string).append(" for next query...").toString());
    }

    protected Query getBooleanQuery(Vector clauses) throws ParseException {
        BooleanQuery query = new BooleanQuery();
        BooleanQuery singleCombined;
        BooleanClause clause;
        BooleanClause combiner;
        Vector v;
        for (int i = 0; i < clauses.size(); i++) {
            clause = (BooleanClause) clauses.elementAt(i);
            if (!clause.prohibited && !clause.required) {
                clause.required = true;
            }
            logger.debug("adding clause: " + clause.query);
            singleCombined = new BooleanQuery();
            combiner = new BooleanClause(WS_PARSER.getFieldQuery(
                    GROUPING_FIELD, groupingValue), true, false);
            singleCombined.add(combiner);
            singleCombined.add(clause);
            v = new Vector();
            v.add(combiner);
            v.add(clause);
            query.add(super.getBooleanQuery(v), false, false);
        }
        return query;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.queryParser.QueryParser#parse(java.lang.String)
     */
    public Query parse(String query) throws ParseException {
        logger.debug("parsing query using: " + analyzer.getClass().getName());
        Query queryTemp = super.parse(query);
        //as a workaround to filter standard lucene query change
        //only queries that search against the standard field..
        if (queryTemp.toString().indexOf(GROUPING_FIELD) == -1) {
            Vector v = new Vector();
            BooleanClause clause = new BooleanClause(queryTemp, true, false);
            v.add(clause);
            return getBooleanQuery(v);
        }
        return queryTemp;
    }

    /**
     * Parses a query String, returning an array of
     * {@link org.apache.lucene.search.BooleanQuery}
     * 
     * Syntax:
     * 
     * <pre>
     * 
     *  foo bar   : search for foo AND bar anywhere across the files of the derivate
     *  foo -bar  : search for foo and no file of the derivate may contain bar
     *  &quot;foo bar&quot; : any file of the derivate must contain the phrase foo bar.
     *  
     * </pre>
     * 
     * @param query
     *            Query matching the given syntax
     * @return Array of BooleanQuery queries against a single derivate
     * @throws ParseException
     *             if syntax mismatches
     */
    public BooleanQuery[] getBooleanQueries(String query) throws ParseException {
        logger.debug("preparsed query:" + parse(query).toString());
        BooleanQuery bQuery = (BooleanQuery) parse(query);
        BooleanClause[] clauses = bQuery.getClauses();
        BooleanQuery[] queries = new BooleanQuery[clauses.length];
        for (int i = 0; i < clauses.length; i++) {
            if (clauses[i].prohibited == true
                    || (clauses.length > 1 && clauses[i].required == true))
                throw new ParseException("Queries should be OR linked: "
                        + clauses[i].prohibited + ":" + clauses[i].required
                        + "\n" + clauses[i].query.toString(field));
            queries[i] = bClauseToBQuery(clauses[i]);
        }
        return queries;
    }

    private final BooleanQuery bClauseToBQuery(BooleanClause clause)
            throws ParseException {
        BooleanQuery query = new BooleanQuery();
        query.add(clause);
        return query;
    }

}