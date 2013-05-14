/**
 * 
 */
package org.mycore.solr.legacy;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.Version;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.MCRNormalizer;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.parsers.bool.MCRSetCondition;
import org.mycore.services.fieldquery.MCRFieldDef;

/**
 * @author shermann
 *
 */
@SuppressWarnings("deprecation")
public class MCRLuceneSolrAdapter extends MCRSolrAdapter {
    /**/
    private static final Logger LOGGER = Logger.getLogger(MCRLuceneSolrAdapter.class);

    protected static Version LUCENE_VERSION = Version.LUCENE_36;

    protected static Analyzer ANALYZER = new StandardAnalyzer(LUCENE_VERSION);

    static {
        BooleanQuery.setMaxClauseCount(10000);
    }

    protected Query handleCondition(String field, String operator, String value, boolean reqf) throws IOException,
        ParseException, org.apache.lucene.queryParser.ParseException {
        LOGGER.debug("field: " + field + " operator: " + operator + " value: " + value);

        String fieldtype = MCRFieldDef.getDef(field).getDataType();
        if ("name".equals(fieldtype)) {
            fieldtype = "text";
        }
        if ("index".equals(fieldtype)) {
            fieldtype = "identifier";
            value = MCRNormalizer.normalizeString(value);
        }
        if ("text".equals(fieldtype) && "contains".equals(operator)) {
            BooleanQuery bq = null;

            Term te;
            TermQuery tq = null;

            TokenStream ts = ANALYZER.tokenStream(field, new StringReader(value));

            while (ts.incrementToken()) {
                TermAttribute ta = ts.getAttribute(TermAttribute.class);
                te = new Term(field, ta.term());

                if (null != tq && null == bq) // not first token
                {
                    bq = new BooleanQuery();
                    if (reqf) {
                        bq.add(tq, BooleanClause.Occur.MUST);
                    } else {
                        bq.add(tq, BooleanClause.Occur.SHOULD);
                    }
                }

                tq = new TermQuery(te);

                if (null != bq) {
                    if (reqf) {
                        bq.add(tq, BooleanClause.Occur.MUST);
                    } else {
                        bq.add(tq, BooleanClause.Occur.SHOULD);
                    }
                }
            }

            if (null != bq) {
                return bq;
            }
            return tq;
        } else if (("text".equals(fieldtype) || "identifier".equals(fieldtype)) && "like".equals(operator)) {
            Term te;

            String help = value.endsWith("*") ? value.substring(0, value.length() - 1) : value;

            if (help.contains("*") || help.contains("?")) {
                LOGGER.debug("WILDCARD");

                te = new Term(field, value);
                return new WildcardQuery(te);
            }

            te = new Term(field, help);
            return new PrefixQuery(te);
        } else if ("text".equals(fieldtype) && ("phrase".equals(operator) || "=".equals(operator))) {
            Term te;
            PhraseQuery pq = new PhraseQuery();
            TokenStream ts = ANALYZER.tokenStream(field, new StringReader(value));

            while (ts.incrementToken()) {
                TermAttribute ta = ts.getAttribute(TermAttribute.class);
                te = new Term(field, ta.term());
                pq.add(te);
            }

            return pq;
        } else if ("text".equals(fieldtype) && "fuzzy".equals(operator)) // 1.9.05
        // future use
        {
            Term te;
            value = fixQuery(value);
            te = new Term(field, value);

            return new FuzzyQuery(te);
        } else if ("text".equals(fieldtype) && "range".equals(operator)) // 1.9.05
        // future use
        {
            String lower = null;
            String upper = null;
            TokenStream ts = ANALYZER.tokenStream(field, new StringReader(value));
            ts.incrementToken();

            TermAttribute ta = ts.getAttribute(TermAttribute.class);
            lower = ta.term();
            if (ts.incrementToken()) {
                ta = ts.getAttribute(TermAttribute.class);
                upper = ta.term();
            }

            return new TermRangeQuery(field, lower, upper, true, true);
        } else if ("date".equals(fieldtype) || "time".equals(fieldtype) || "timestamp".equals(fieldtype)) {
            return generateDateQuery(field, operator, value);
        } else if ("identifier".equals(fieldtype) && "=".equals(operator)) {
            Term te = new Term(field, "\"" + value + "\"");

            return new TermQuery(te);
        } else if ("boolean".equals(fieldtype)) {
            Term te = new Term(field, "true".equals(value) ? "1" : "0");

            return new TermQuery(te);
        } else if ("decimal".equals(fieldtype)) {
            return NumberQuery(field, "decimal", operator, Float.parseFloat(value));
        } else if ("integer".equals(fieldtype)) {
            return NumberQuery(field, "integer", operator, Long.parseLong(value));
        } else if ("text".equals(fieldtype) && "lucene".equals(operator)) // value
        // contains query for lucene, use query parser
        {
            QueryParser qp = new QueryParser(LUCENE_VERSION, field, ANALYZER);
            Query query = qp.parse(fixQuery(value));

            LOGGER.debug("Lucene query: " + query.toString());

            return query;
        } else if (("text".equals(fieldtype) || "identifier".equals(fieldtype) || "index".equals(fieldtype))
            && ("<".equals(operator) || "<=".equals(operator) || ">=".equals(operator) || ">".equals(operator))) {
            return TermInequalityQuery(field, fieldtype, operator, value);
        }

        else {
            LOGGER.info("Not supported, fieldtype: " + fieldtype + " operator: " + operator);
        }

        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected String getQueryString(MCRCondition condition) {
        boolean required = true;
        List<Element> f = new ArrayList<Element>();
        if (condition instanceof MCRSetCondition) {
            //special handling to strip away surrounding "+()"
            @SuppressWarnings("unchecked")
            List<MCRCondition> children = ((MCRSetCondition) condition).getChildren();
            for (MCRCondition child : children) {
                f.add(child.toXML());
            }
            required = !(condition instanceof MCROrCondition);
        } else {
            f.add(condition.toXML());
        }
        Query luceneQuery;
        try {
            luceneQuery = buildLuceneQuery(null, required, f, new HashSet<String>());
        } catch (IOException | ParseException | org.apache.lucene.queryParser.ParseException e) {
            throw new MCRException("Error while building SOLR query.", e);
        }

        String queryString = luceneQuery.toString();
        return queryString;
    }

    /**
     * Build Lucene Query from XML
     * 
     * @return Lucene Query
     * @throws ParseException 
     * @throws org.apache.lucene.queryParser.ParseException 
     * @throws IOException 
     * 
     */
    protected Query buildLuceneQuery(BooleanQuery r, boolean reqf, List<Element> f, Set<String> usedFields)
        throws IOException, ParseException, org.apache.lucene.queryParser.ParseException {
        for (Element xEle : f) {
            String name = xEle.getName();
            if ("boolean".equals(name)) {
                name = xEle.getAttributeValue("operator").toLowerCase();
            }
            Query x = null;

            boolean reqfn = reqf;
            boolean prof = false;

            List<Element> children = xEle.getChildren();
            if (name.equals("and")) {
                x = buildLuceneQuery(null, true, children, usedFields);
            } else if (name.equalsIgnoreCase("or")) {
                x = buildLuceneQuery(null, false, children, usedFields);
            } else if (name.equalsIgnoreCase("not")) {
                x = buildLuceneQuery(null, false, children, usedFields);
                reqfn = false; // javadoc lucene: It is an error to specify a
                // clause as both required and prohibited
                prof = true;
            } else if (name.equalsIgnoreCase("condition")) {
                String field = xEle.getAttributeValue("field", "").intern();
                String operator = xEle.getAttributeValue("operator", "").intern();
                String value = xEle.getAttributeValue("value", "");
                if (usedFields != null) {
                    usedFields.add(field);
                }
                x = handleCondition(field, operator, value, reqf);
            }
            if (null != x) {
                if (null == r) {
                    r = new BooleanQuery();
                }
                BooleanClause.Occur occur = BooleanClause.Occur.MUST;
                if (reqfn && !prof) {
                } else if (!reqfn && !prof) {
                    occur = BooleanClause.Occur.SHOULD;
                } else if (!reqfn && prof) {
                    occur = BooleanClause.Occur.MUST_NOT;
                }
                BooleanClause bq = new BooleanClause(x, occur);
                r.add(bq);
            }
        }
        return r;
    }

    protected Query getTermRangeQuery(String fieldname, String op, String value) {
        if (value == null) {
            return null;
        }
        if (op.contains(">")) {
            return new TermRangeQuery(fieldname, value, null, op.length() == 2, true);
        } else if (op.contains("<")) {
            return new TermRangeQuery(fieldname, null, value, true, op.length() == 2);
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
        StringBuilder _fixedQuery = new StringBuilder(aQuery.length());
        boolean _inString = false;

        while (_tokenizer.hasMoreTokens()) {
            String _token = _tokenizer.nextToken();

            if (!"NOT".equals(_token) && !"AND".equals(_token) && !"OR".equals(_token) && !"TO".equals(_token)
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
    private static Query NumberQuery(String fieldname, String type, String Op, Number value) {
        if (value == null) {
            return null;
        }
        if (type.equals("decimal")) {
            float valueNumber = value.floatValue();
            float lower = 0.0f - Float.MAX_VALUE;
            float upper = Float.MAX_VALUE;
            if (Op.equals(">") || Op.equals(">=")) {
                lower = valueNumber;
                return NumericRangeQuery.newFloatRange(fieldname, lower, upper, Op.length() == 2, true);
            }
            if (Op.equals("<") || Op.equals("<=")) {
                upper = valueNumber;
                return NumericRangeQuery.newFloatRange(fieldname, lower, upper, true, Op.length() == 2);
            }
            if (Op.equals("=")) {
                return NumericRangeQuery.newFloatRange(fieldname, valueNumber, valueNumber, true, true);
            }
        }
        if (type.equals("integer")) {
            long valueNumber = value.longValue();
            long lower = Long.MIN_VALUE;
            long upper = Long.MAX_VALUE;
            if (Op.equals(">") || Op.equals(">=")) {
                lower = valueNumber;
                return NumericRangeQuery.newLongRange(fieldname, lower, upper, Op.length() == 2, true);
            }
            if (Op.equals("<") || Op.equals("<=")) {
                upper = valueNumber;
                return NumericRangeQuery.newLongRange(fieldname, lower, upper, true, Op.length() == 2);
            }
            if (Op.equals("=")) {
                return NumericRangeQuery.newLongRange(fieldname, valueNumber, valueNumber, true, true);
            }
        }
        LOGGER.info("Invalid operator for Number: " + Op);

        return null;
    }

    /***************************************************************************
     * TermInequalityQuery ()
     * deals with operators: <, <=, =>, >
     * for field types: text index, identifier
     **************************************************************************/
    protected static Query TermInequalityQuery(String fieldname, String type, String op, String value) {
        if (value == null) {
            return null;
        }
        if (type.equals("text") || type.equals("identifier") || type.equals("index")) {
            String lower = null;
            String upper = null;
            if (op.equals(">") || op.equals(">=")) {
                lower = value;
                return new TermRangeQuery(fieldname, lower, upper, op.length() == 2, true);
            }
            if (op.equals("<") || op.equals("<=")) {
                upper = value;
                return new TermRangeQuery(fieldname, lower, upper, true, op.length() == 2);
            }
            if (op.equals("=")) {
                return new TermRangeQuery(fieldname, value, value, true, true);
            }
        }
        LOGGER.info("Invalid operator: " + op);

        return null;
    }

    private static Query generateDateQuery(String fieldname, String Op, String value) throws ParseException {
        long numberValue = getLongValue(value);
        return NumberQuery(fieldname, "integer", Op, numberValue);
    }

    private static final int HH_MM_SS = 8;

    private static final int YYYY_MM_DD_HH_MM_SS = 19;

    private static long getLongValue(String isoDate) throws ParseException {
        switch (isoDate.length()) {
            case YYYY_MM_DD_HH_MM_SS:
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                return df.parse(isoDate).getTime();
            case HH_MM_SS:
                short hour = Short.parseShort(isoDate.substring(0, 2));
                short minute = Short.parseShort(isoDate.substring(3, 5));
                short second = Short.parseShort(isoDate.substring(6));
                return (hour * 60 + minute) * 60 + second;
            default:
                MCRISO8601Date date = new MCRISO8601Date(isoDate);
                if (date.getDate() == null) {
                    throw new MCRException("Could not parse to long value: " + isoDate);
                }
                return date.getDate().getTime();
        }
    }
}
