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
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreRangeQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.mycore.common.MCRUtils;
import org.mycore.services.fieldquery.MCRFieldDef;

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

    static
    {
      BooleanQuery.setMaxClauseCount( 10000 );
    }
    
    static Hashtable search = null;

    /**
     * Build Lucene Query from XML
     * 
     * @return Lucene Query
     * 
     */
    public static Query buildLuceneQuery(BooleanQuery r, boolean reqf, List f, Analyzer analyzer) throws Exception {
        for (int i = 0; i < f.size(); i++) {
            org.jdom.Element xEle = (org.jdom.Element) (f.get(i));
            String name = xEle.getName();
            if ("boolean".equals(name))
              name = xEle.getAttributeValue("operator").toLowerCase();
            Query x = null;

            boolean reqfn = reqf;
            boolean prof = false;

            if (name.equals("and")) {
                x = buildLuceneQuery(null, true, xEle.getChildren(), analyzer);
            } else if (name.equalsIgnoreCase("or")) {
                x = buildLuceneQuery(null, false, xEle.getChildren(), analyzer);
            } else if (name.equalsIgnoreCase("not")) {
                x = buildLuceneQuery(null, false, xEle.getChildren(), analyzer);
                reqfn = false; // javadoc lucene: It is an error to specify a
                                // clause as both required and prohibited
                prof = true;
            } else if (name.equalsIgnoreCase("condition")) {
                String field = xEle.getAttributeValue("field", "");
                String operator = xEle.getAttributeValue("operator", "");
                String value = xEle.getAttributeValue("value", "");

                LOGGER.debug("field: " + field + " operator: " + operator + " value: " + value);

                String fieldtype = MCRFieldDef.getDef( field ).getDataType();

                if ("name".equals(fieldtype)) {
                    fieldtype = "text";
                }

                x = handleCondition(field, operator, value, fieldtype, reqf, analyzer);
            }

            if (null != x) {
                if (null == r) {
                    r = new BooleanQuery();
                }

                //BooleanClause bq = new BooleanClause(x, reqfn, prof);
                BooleanClause.Occur occur = BooleanClause.Occur.MUST;
                
                if (reqfn && !prof )
                  ;
                else if ( !reqfn && !prof)
                  occur = BooleanClause.Occur.SHOULD; 
                else if ( !reqfn && prof)
                  occur = BooleanClause.Occur.MUST_NOT; 
                BooleanClause bq = new BooleanClause(x, occur);
                r.add(bq);
            }
        } // for

        return r;
    }

    private static Query handleCondition(String field, String operator, String value, String fieldtype, boolean reqf, Analyzer analyzer) throws Exception {
        if ("text".equals(fieldtype) && "contains".equals(operator)) {
            BooleanQuery bq = null;

            Term te;
            TermQuery tq = null;

            TokenStream ts = analyzer.tokenStream(field, new StringReader(value));
            Token to;

            while ((to = ts.next()) != null) {
                te = new Term(field, to.termText());

                if ((null != tq) && (null == bq)) // not first token
                {
                    bq = new BooleanQuery();
                    //bq.add(tq, reqf, false);
                    if (reqf)
                      bq.add(tq, BooleanClause.Occur.MUST);
                    else
                      bq.add(tq, BooleanClause.Occur.SHOULD);
                }

                tq = new TermQuery(te);

                if (null != bq) {
                    //bq.add(tq, reqf, false);
                    if (reqf)
                      bq.add(tq, BooleanClause.Occur.MUST);
                    else
                      bq.add(tq, BooleanClause.Occur.SHOULD);
                }
            }

            if (null != bq) {
                return bq;
            }
            return tq;
        } else if (("text".equals(fieldtype) || "identifier".equals(fieldtype)) && "like".equals(operator)) {
            Term te;
            
            String help = value.endsWith("*") ? value.substring(0, value.length()-1) : value;

            if ((-1 != help.indexOf("*")) || (-1 != help.indexOf("?"))) {
                LOGGER.debug("WILDCARD");

                te = new Term(field, value);
                return new WildcardQuery(te);
            }

            te = new Term(field, help);
            return new PrefixQuery(te);
        } else if ("text".equals(fieldtype) && ("phrase".equals(operator) || "=".equals(operator))) {
            Term te;
            PhraseQuery pq = new PhraseQuery();
            TokenStream ts = analyzer.tokenStream(field, new StringReader(value));
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
            String lower = null;
            String upper = null;
            TokenStream ts = analyzer.tokenStream(field, new StringReader(value));
            Token to;

            to = ts.next();
            lower = to.termText();
            to = ts.next();

            if (null != to) {
                upper = to.termText();
            }

            return new ConstantScoreRangeQuery(field, lower, upper, true, true);
        } else if ("date".equals(fieldtype) || "time".equals(fieldtype) || "timestamp".equals(fieldtype)) {
            return DateQuery2(field, operator, value);
        } else if ("identifier".equals(fieldtype) && "=".equals(operator)) {
            Term te = new Term(field, value);

            return new TermQuery(te);
        } else if ("boolean".equals(fieldtype) ) {
            Term te = new Term(field, "true".equals(value) ? "1" : "0");

            return new TermQuery(te);
        } else if ("decimal".equals(fieldtype)) {  
          return NumberQuery(field, "decimal", operator, value);
        } else if ("integer".equals(fieldtype)) {  
          return NumberQuery(field, "integer", operator, value);
        } else if ("text".equals(fieldtype) && "lucene".equals(operator)) // value
                                                                            // contains
                                                                            // query
                                                                            // for
                                                                            // lucene,
        // use query parser
        {
            QueryParser qp = new QueryParser(field, analyzer);
            Query query = qp.parse( fixQuery(value) );
            
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
    public static String fixQuery(String aQuery) {
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
     * NumberQuery ()
     **************************************************************************/
    private static Query NumberQuery(String fieldname, String type, String Op, String value) throws Exception {
        if (value.length() == 0) {
            return null;
        }
        
        String lower = "0000000000000000000";
        String upper = "9999999999999999999";
        
        if (Op.equals(">") || Op.equals(">=") ) {
          lower = MCRLuceneSearcher.handleNumber(value, type, Op.equals(">") ? 1 : 0);
          upper = upper.substring(0, lower.length() );
      } else if (Op.equals("<") || Op.equals("<=") ) {
          upper = MCRLuceneSearcher.handleNumber(value, type, Op.equals("<") ? -1 : 0);
          lower = lower.substring(0, upper.length() );
      } else if (Op.equals("=")) {
          return new TermQuery(new Term(fieldname, MCRLuceneSearcher.handleNumber(value, type, 0)));
      } else {
          LOGGER.info("Invalid operator for Number: " + Op);

          return null;
      }

      return new ConstantScoreRangeQuery(fieldname, lower, upper, true, true);
    }
    
    /***************************************************************************
     * DateQuery2 ()
     **************************************************************************/
    private static Query DateQuery2(String fieldname,  String Op, String value) {
        if (value.length() == 0) {
            return null;
        }
        
        String lower = null;
        String upper = null;
        
        if (Op.equals(">") || Op.equals(">=") ) {
          lower = value;
      } else if (Op.equals("<") || Op.equals("<=") ) {
          upper = value;
      } else if (Op.equals("=")) {
          return new TermQuery( new Term(fieldname, value) );
      } else {
          LOGGER.info("Invalid operator for Number: " + Op);

          return null;
      }

      boolean incl = Op.equals(">=") || Op.equals("<=") ? true : false;   
//      return new RangeQuery( lower, upper, incl);
      return new ConstantScoreRangeQuery(fieldname, lower, upper, incl, incl);
    }
}
