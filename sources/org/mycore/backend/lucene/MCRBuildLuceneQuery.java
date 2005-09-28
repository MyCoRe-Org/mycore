/*
 * $RCSfile$
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

package org.mycore.backend.lucene;

import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.mycore.common.MCRUtils;
import org.mycore.services.fieldquery.MCRSearchField;

/**
 * This class builds a Lucene Query from XML query (specified by Frank
 * Lützenkirchen)
 * 
 * @author Harald Richter
 * 
 * @version $Revision$ $Date$
 * 
 */
public class MCRBuildLuceneQuery {
    private static final Logger LOGGER = Logger.getLogger(MCRBuildLuceneQuery.class);

    // TODO: read from property file
    static String DATE_FORMAT = "yyyy-MM-dd";

    static String TIME_FORMAT = "hh:mm:ss";

    static String TIMESTAMP_FORMAT = "yyyy-MM-dd hh:mm:ss";

    static Analyzer analyzer = new GermanAnalyzer();

    static Hashtable search = null;

    /**
     * Build Lucene Query from XML
     * 
     * @param query
     *            as xml
     * 
     * @return Lucene Query
     * 
     */
    public static Query buildLuceneQuery(BooleanQuery r, boolean reqf, List f) throws Exception {
        for (int i = 0; i < f.size(); i++) {
            org.jdom.Element xEle = (org.jdom.Element) (f.get(i));
            String name = xEle.getName();
            Query x = null;
            ;

            boolean reqfn = reqf;
            boolean prof = false;

            if (name.equals("and")) {
                x = buildLuceneQuery(null, true, xEle.getChildren());
            } else if (name.equalsIgnoreCase("or")) {
                x = buildLuceneQuery(null, false, xEle.getChildren());
            } else if (name.equalsIgnoreCase("not")) {
                x = buildLuceneQuery(null, false, xEle.getChildren());
                reqfn = false; // javadoc lucene: It is an error to specify a
                                // clause as both required and prohibited
                prof = true;
            } else if (name.equalsIgnoreCase("condition")) {
                String field = xEle.getAttributeValue("field", "");
                String operator = xEle.getAttributeValue("operator", "");
                String value = xEle.getAttributeValue("value", "");

                LOGGER.debug("field: " + field + " operator: " + operator + " value: " + value);

                String fieldtype = MCRSearchField.getDataType(field);

                if ("name".equals(fieldtype)) {
                    fieldtype = "text";
                }

                x = handleCondition(field, operator, value, fieldtype, reqf);
            }

            if (null != x) {
                if (null == r) {
                    r = new BooleanQuery();
                }

                BooleanClause bq = new BooleanClause(x, reqfn, prof);
                r.add(bq);
            }
        } // for

        return r;
    }

    private static Query handleCondition(String field, String operator, String value, String fieldtype, boolean reqf) throws Exception {
        if ("text".equals(fieldtype) && ("contains".equals(operator) || "=".equals(operator))) {
            BooleanQuery bq = null;

            Term te;
            TermQuery tq = null;

            TokenStream ts = analyzer.tokenStream(null, new StringReader(value));
            Token to;

            while ((to = ts.next()) != null) {
                te = new Term(field, to.termText());

                if ((null != tq) && (null == bq)) // not first token
                {
                    bq = new BooleanQuery();
                    bq.add(tq, reqf, false);
                }

                tq = new TermQuery(te);

                if (null != bq) {
                    bq.add(tq, reqf, false);
                }
            }

            if (null != bq) {
                return bq;
            } else {
                return tq;
            }
        } else if ("text".equals(fieldtype) && "like".equals(operator)) {
            Term te;
            value = fixQuery(value);
            te = new Term(field, value);

            if ((-1 != value.indexOf("*")) || (-1 != value.indexOf("?"))) {
                LOGGER.debug("WILDCARD");

                return new WildcardQuery(te);
            }

            return new PrefixQuery(te);
        } else if ("text".equals(fieldtype) && "phrase".equals(operator)) {
            Term te;
            PhraseQuery pq = new PhraseQuery();
            TokenStream ts = analyzer.tokenStream(null, new StringReader(value));
            Token to;

            while ((to = ts.next()) != null) {
                te = new Term(field, to.termText());
                pq.add(te);
            }

            return pq;
        } else if ("text".equals(fieldtype) && "fuzzy".equals(operator)) // 1.9.05
                                                                            // future
                                                                            // use
        {
            Term te;
            value = fixQuery(value);
            te = new Term(field, value);

            return new FuzzyQuery(te);
        } else if ("text".equals(fieldtype) && "range".equals(operator)) // 1.9.05
                                                                            // future
                                                                            // use
        {
            Term lower = null;
            Term upper = null;
            TokenStream ts = analyzer.tokenStream(null, new StringReader(value));
            Token to;

            to = ts.next();
            lower = new Term(field, to.termText());
            to = ts.next();

            if (null != to) {
                upper = new Term(field, to.termText());
            }

            return new RangeQuery(lower, upper, true);
        } else if ("date".equals(fieldtype)) {
            return DateQuery(field, DATE_FORMAT, "yyyyMMdd", operator, value);
        } else if ("time".equals(fieldtype)) {
            return DateQuery(field, TIME_FORMAT, "HHmmss", operator, value);
        } else if ("timestamp".equals(fieldtype)) {
            return DateQuery(field, TIMESTAMP_FORMAT, "yyyyMMddHHmmss", operator, value);
        } else if ("identifier".equals(fieldtype) && "=".equals(operator)) {
            Term te = new Term(field, value);

            return new TermQuery(te);
        } else if ("text".equals(fieldtype) && "lucene".equals(operator)) // value
                                                                            // contains
                                                                            // query
                                                                            // for
                                                                            // lucene,
        // use query parser
        {
            Query query = QueryParser.parse(field + ":(" + fixQuery(value) + ")", "", analyzer);
            LOGGER.debug("Lucene query: " + query.toString());

            return query;
        } else {
            LOGGER.info("Not supported, fieldtype: " + fieldtype + " operator: " + operator);
        }

        return null;
    }

    // code from Otis Gospodnetic http://www.jguru.com/faq/view.jsp?EID=538312
    // Question Are Wildcard, Prefix, and Fuzzy queries case sensitive?
    // Yes, unlike other types of Lucene queries, Wildcard, Prefix, and Fuzzy
    // queries are case sensitive. That is because those types of queries are
    // not passed through the Analyzer, which is the component that performs
    // operations such as stemming and lowercasing.
    private static String fixQuery(String aQuery) {
        aQuery = MCRUtils.replaceString(aQuery, "'", "\""); // handle phrase

        StringTokenizer _tokenizer = new StringTokenizer(aQuery, " \t\n\r", true);
        StringBuffer _fixedQuery = new StringBuffer(aQuery.length());
        boolean _inString = false;

        while (_tokenizer.hasMoreTokens()) {
            String _token = _tokenizer.nextToken();

            if ((!"NOT".equals(_token) && !"AND".equals(_token) && !"OR".equals(_token) && !"TO".equals(_token)) || _inString) {
                _fixedQuery.append(_token.toLowerCase());
            } else {
                _fixedQuery.append(_token);
            }

            int _nbQuotes = count(_token, "\""); // Count the "
            int _nbEscapedQuotes = count(_token, "\\\""); // Count the \"

            if (((_nbQuotes - _nbEscapedQuotes) % 2) != 0) {
                // there is an odd number of string delimiters
                _inString = !_inString;
            }
        }

        String qu = _fixedQuery.toString();
        qu = MCRUtils.replaceString(qu, "ä", "a");
        qu = MCRUtils.replaceString(qu, "ö", "o");
        qu = MCRUtils.replaceString(qu, "ü", "u");
        qu = MCRUtils.replaceString(qu, "ß", "ss");

        return qu;
    }

    private static int count(String aSourceString, String aCountString) {
        int fromIndex = 0;
        int foundIndex = 0;
        int count = 0;

        while ((foundIndex = aSourceString.indexOf(aCountString, fromIndex)) > -1) {
            count++;
            fromIndex = ++foundIndex;
        }

        return count;
    }

    /***************************************************************************
     * DateQuery ()
     **************************************************************************/
    private static Query DateQuery(String fieldname, String informat, String outformat, String dateOp, String date) throws Exception {
        if (date.length() == 0) {
            return null;
        }

        dateOp = dateOp.trim();

        if (dateOp.equals("==")) {
            dateOp = "=";
        }

        try {
            DateFormat f1 = new SimpleDateFormat(informat);
            DateFormat f2 = new SimpleDateFormat(outformat);
            Date d = f1.parse(date);
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTime(d);

            int len = outformat.length();

            String lower = "00000000000000";
            lower = lower.substring(0, len);

            String upper = "99999999999999";
            upper = upper.substring(0, len);

            if (dateOp.equals(">")) {
                if (8 == len) { // date
                    gc.add(Calendar.DAY_OF_MONTH, 1);
                } else { // time or timestamp
                    gc.add(Calendar.SECOND, 1);
                }

                d = gc.getTime();
                lower = f2.format(d);
            } else if (dateOp.equals("<")) {
                if (8 == len) { // date
                    gc.add(Calendar.DAY_OF_MONTH, -1);
                } else { // time or timestamp
                    gc.add(Calendar.SECOND, -1);
                }

                d = gc.getTime();
                upper = f2.format(d);
            } else if (dateOp.equals("=")) {
                return new TermQuery(new Term(fieldname, f2.format(d)));
            } else if (dateOp.equals(">=")) {
                lower = f2.format(d);
            } else if (dateOp.equals("<=")) {
                upper = f2.format(d);
            } else {
                LOGGER.info("Invalid operator for date: " + dateOp);

                return null;
            }

            return new RangeQuery(new Term(fieldname, lower), new Term(fieldname, upper), true);
        } catch (ParseException e) {
            LOGGER.info("invalid date: " + date);

            return null;
        }
    }
}
