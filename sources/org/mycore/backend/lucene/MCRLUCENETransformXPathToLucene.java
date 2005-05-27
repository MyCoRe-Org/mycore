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

import java.io.File;
import java.util.*;
import java.text.*;

import org.apache.log4j.Logger;
import org.mycore.common.*;
//import org.mycore.common.MCRConfiguration;
//import org.mycore.common.MCRPersistenceException;
//import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.query.MCRMetaSearchInterface;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.queryParser.QueryParser;

import org.apache.lucene.search.BooleanQuery;

/**
 * This is the implementation of the MCRMetaSearchInterface with Lucene
 * 
 * @author Harald Richter
 * @version $Revision$ $Date$
 */
public class MCRLUCENETransformXPathToLucene implements MCRMetaSearchInterface {

    /** The default query * */
    public static final String DEFAULT_QUERY = "/*";

    // the logger
    protected static Logger logger = Logger
            .getLogger(MCRLUCENETransformXPathToLucene.class.getName());

    private MCRConfiguration config = null;

    static String indexDir = "";

    /**
     * The constructor.
     */
    public MCRLUCENETransformXPathToLucene() {
        config = MCRConfiguration.instance();
        indexDir = config.getString("MCR.persistence_lucene_searchindexdir");
        logger.debug("MCR.persistence_lucene_searchindexdir: " + indexDir);
        String lockDir = config.getString("MCR.persistence_lucene_lockdir", "");
        logger.info("MCR.persistence_lucene_lockdir: " + lockDir);
        File file = new File(lockDir);

        if (!file.exists()) {
            logger.info("Lock Directory for Lucene doesn't exist: \"" + lockDir
                    + "\" use " + System.getProperty("java.io.tmpdir"));
        } else if (file.isDirectory()) {
            System.setProperty("org.apache.lucene.lockdir", lockDir);
        }
    }

    /**
     * This method starts the Query over the Lucene persistence layer for one
     * object type and and returns the query result as a HashSet of
     * MCRObjectIDs.
     * 
     * @param root
     *            the query root
     * @param query
     *            the metadata queries
     * @param type
     *            the MCRObject type
     * @return a result list as MCRXMLContainer
     */
    public final HashSet getResultIDs(String root, String query, String type) {
        // prepare the query over the rest of the metadata
        HashSet idmeta = new HashSet();
        logger.debug("Incomming condition : " + query);
        Vector newquery;
        if ((root == null) && (query.length() == 0)) {
            //TODO newquery = DEFAULT_QUERY;
        }
        newquery = handleQueryStringLucene(root, query, type);
        logger.debug("Transformed query for Lucene: " + newquery);

        // do it over the metadata
        if (newquery.size() != 0) {
            try {
                long start = System.currentTimeMillis();
                IndexSearcher searcher = new IndexSearcher(indexDir);
                Analyzer analyzer = new GermanAnalyzer();

                BooleanQuery bq = new BooleanQuery();
                Query qu;

                for (int i = 0; i < newquery.size(); i++) {
                    SearchField sf = (SearchField) newquery.elementAt(i);
                    String s = sf.name;
                    if (sf.type.equals("UnStored")) {
                        qu = QueryParser.parse(sf.name + ":"
                                + fixQuery(sf.content), "", analyzer);
                        bq.add(qu, true, false);
                    } else if (sf.type.equals("Keyword")) {
                        String s2 = sf.content;
                        if (s2.endsWith("*")) {
                            s2 = s2.substring(0, s2.length() - 1);
                            Term te = new Term(sf.name, s2);
                            PrefixQuery pq = new PrefixQuery(te);
                            bq.add(pq, true, false);
                        } else {
                            Term te = new Term(sf.name, s2);
                            TermQuery tq = new TermQuery(te);
                            bq.add(tq, true, false);
                        }
                    } else if (sf.type.equals("date")) {
                        String text = getCondDate("dd.MM.yyyy", sf.op,
                                sf.content);
                        qu = QueryParser.parse(sf.name + ":" + text, "",
                                analyzer);
                        bq.add(qu, true, false);
                    }
                }

                Term te = new Term("mcr_type", type);
                TermQuery tq = new TermQuery(te);
                bq.add(tq, true, false);

                logger.debug("Searching for: " + bq.toString(""));

                Hits hits = searcher.search(bq);
                long qtime = System.currentTimeMillis() - start;
                start = System.currentTimeMillis();

                logger.debug("+++++Number of objects found : " + hits.length());
                for (int i = 0; i < hits.length(); i++) {
                    String objid = hits.doc(i).get("mcr_ID");
                    logger.debug("+++++Objid: " + objid + " Type: " + type);
                    idmeta.add(new MCRObjectID(objid));
                }

                logger.debug("query time:    " + qtime);
            } catch (Exception e) {
                throw new MCRPersistenceException(e.getMessage(), e);
            }
        }
        return idmeta;
    }

    /**
     * Handle query string for Lucene
     */
    private Vector handleQueryStringLucene(String root, String query,
            String type) {
        Vector searchField = new Vector();
        query = MCRUtils.replaceString(query, "#####", "");
        query = MCRUtils.replaceString(query, "]", "");
        query = MCRUtils.replaceString(query, "metadata[", "");
        query = MCRUtils.replaceString(query, "/mycoreobject[", "");
        query = MCRUtils.replaceString(query, " and ", "#####");
        try {
            query = new String(query.getBytes("ISO-8859-1"), "UTF-8");
        } catch (Exception ex) {
        }
        logger.info(query);

        StringTokenizer st = new StringTokenizer(query, "#####");
        int anz = st.countTokens();
        for (int i = 0; i < anz; i++) {
            String s = st.nextToken().trim();
            String op = "";
            int k = s.indexOf("OP:");
            if (-1 != k) {
                op = s.substring(k + 3);
                s = s.substring(0, k);
            }

            String sftype = "";
            String name = "";
            String content = "";

            if (s.startsWith("TF:")) {
                sftype = "UnStored";
                s = s.substring(3);
            } else if (s.startsWith("CL:")) {
                sftype = "Keyword";
                s = s.substring(3);
            } else if (s.startsWith("DF:")) {
                sftype = "date";
                s = s.substring(3);
            } else {/* TODO ERROR Handling */
            }

            k = s.indexOf(":");
            if (-1 != k) {
                name = s.substring(0, k);
                content = s.substring(k + 1);
            }

            SearchField sf = new SearchField();
            sf.name = name;
            sf.type = sftype; // text, date, ...
            sf.content = content;
            sf.op = op;
            searchField.add(sf);
            logger.info("SearchField  Name: " + sf.name + " type: " + sf.type
                    + " content: " + sf.content + " op: " + sf.op);
        }

        //  MCRSessionMgr.getCurrentSession();

        return searchField;
    }

    class SearchField {
        String name;

        String type;

        String content;

        String op;
    }

    private String getCondDate(String format, String dateOp, String date)
            throws Exception {
        if (date.length() == 0)
            return "";

        dateOp = dateOp.trim();
        if (dateOp.equals("=="))
            dateOp = "=";
        try {
            DateFormat f1 = new SimpleDateFormat(format);
            DateFormat f2 = new SimpleDateFormat("yyyyMMdd");
            Date d = f1.parse(date);
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTime(d);

            if (dateOp.equals(">")) {
                gc.add(Calendar.DAY_OF_MONTH, 1);
                d = gc.getTime();
                return "[" + f2.format(d) + " TO 99999999]";
            } else if (dateOp.equals("<")) {
                gc.add(Calendar.DAY_OF_MONTH, -1);
                d = gc.getTime();
                return "[00000000 TO " + f2.format(d) + "]";
            } else if (dateOp.equals("=")) {
                return f2.format(d);
            } else if (dateOp.equals(">=")) {
                return "[" + f2.format(d) + " TO 99999999]";
            } else if (dateOp.equals("<=")) {
                return "[00000000 TO " + f2.format(d) + "]";
            } else {
                System.out.println("invalid comparetype for date: " + dateOp);
                return "";
            }
        } catch (ParseException e) {
            //e.printStackTrace();
            System.out.println("invalid date: " + date);
            return "";
        }
    }

    // code from Otis Gospodnetic http://www.jguru.com/faq/view.jsp?EID=538312
    //Question Are Wildcard, Prefix, and Fuzzy queries case sensitive?
    //Yes, unlike other types of Lucene queries, Wildcard, Prefix, and Fuzzy
    //queries are case sensitive. That is because those types of queries are
    //not passed through the Analyzer, which is the component that performs
    //operations such as stemming and lowercasing.
    private String fixQuery(String aQuery) {
        aQuery = MCRUtils.replaceString(aQuery, "'", "\""); // handle phrase

        StringTokenizer _tokenizer = new StringTokenizer(aQuery, " \t\n\r",
                true);
        StringBuffer _fixedQuery = new StringBuffer(aQuery.length());
        boolean _inString = false;
        while (_tokenizer.hasMoreTokens()) {
            String _token = _tokenizer.nextToken();
            if (!"NOT".equals(_token) && !"AND".equals(_token)
                    && !"OR".equals(_token) && !"TO".equals(_token)
                    || _inString) {
                _fixedQuery.append(_token.toLowerCase());
            } else {
                _fixedQuery.append(_token);
            }
            int _nbQuotes = count(_token, "\""); // Count the "
            int _nbEscapedQuotes = count(_token, "\\\""); // Count the \"
            if ((_nbQuotes - _nbEscapedQuotes) % 2 != 0) {
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

    private int count(String aSourceString, String aCountString) {
        int fromIndex = 0;
        int foundIndex = 0;
        int count = 0;
        while ((foundIndex = aSourceString.indexOf(aCountString, fromIndex)) > -1) {
            count++;
            fromIndex = ++foundIndex;
        }
        return count;
    }

}