package org.mycore.solr.legacy;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.util.Version;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.parsers.bool.MCRSetCondition;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSortBy;
import org.mycore.solr.MCRSolrServerFactory;

public class MCRSolrAdapter {

    protected static Version LUCENE_VERSION = Version.LUCENE_36;

    protected static Analyzer ANALYZER = new StandardAnalyzer(LUCENE_VERSION);

    private static final Logger LOGGER = Logger.getLogger(MCRSolrAdapter.class);

    static {
        BooleanQuery.setMaxClauseCount(10000);
    }

    @SuppressWarnings("rawtypes")
    public MCRResults search(MCRCondition condition, int maxResults, List<MCRSortBy> sortBy, boolean addSortData) {
        MCRSolrResults solrResults = null;
        if (maxResults == 0) {
            LOGGER.debug("maxResults should be explicitly set. Try to use paging.");
        }
        try {
            SolrQuery q = getSolrQuery(condition, sortBy, maxResults);
            solrResults = getResults(q);
            LOGGER.debug(solrResults.getNumHits() + " document(s) found");
        } catch (Exception e) {
            LOGGER.error("Exception in while processing legacy lucene query:", e);
        }
        return solrResults != null ? solrResults : new MCRResults();
    }

    public MCRSolrResults getResults(SolrQuery q) throws SolrServerException {
        SolrServer solrServer = MCRSolrServerFactory.getSolrServer();
        SolrDocumentList solrDocumentList = solrServer.query(q).getResults();
        return new MCRSolrResults(solrDocumentList);
    }

    /**
     * @param condition
     * @param sortBy
     * @param maxResults
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SolrQuery getSolrQuery(MCRCondition condition, List<MCRSortBy> sortBy, int maxResults) {
        boolean required = true;
        List<Element> f = new ArrayList<Element>();
        if (condition instanceof MCRSetCondition) {
            //special case to strip away surrounding "+()"
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

        SolrQuery q = applySortOptions(new SolrQuery(luceneQuery.toString()), sortBy);
        q.setIncludeScore(true);
        q.setRows(maxResults == 0 ? Integer.MAX_VALUE : maxResults);

        String sort = q.getSortField();
        LOGGER.info("Legacy Query transformed by " + getClass().getName() + " to: " + q.getQuery() + (sort != null ? " " + sort : ""));
        return q;
    }

    /**
     * @param q
     * @param sortBy
     * @return
     */
    protected SolrQuery applySortOptions(SolrQuery q, List<MCRSortBy> sortBy) {
        for (MCRSortBy option : sortBy) {
            SortClause sortClause = new SortClause(option.getFieldName(), option.getSortOrder() ? ORDER.asc : ORDER.desc);
            q.addSort(sortClause);
        }
        return q;
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
    protected Query buildLuceneQuery(BooleanQuery r, boolean reqf, List<Element> f, Set<String> usedFields) throws IOException, ParseException,
            org.apache.lucene.queryParser.ParseException {
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

    protected Query handleCondition(String field, String operator, String value, boolean required) throws IOException, ParseException,
            org.apache.lucene.queryParser.ParseException {
        if (operator.equals("=") || operator.equals("like") || operator.equals("contains")) {
            return new TermQuery(new Term(field, value));
        } else if (operator.contains(">") || operator.contains("<")) {
            return getTermRangeQuery(field, operator, value);
        } else if (operator.equals("phrase")) {
            PhraseQuery pq = new PhraseQuery();
            TokenStream ts = ANALYZER.tokenStream(field, new StringReader(value));
            while (ts.incrementToken()) {
                CharTermAttribute ta = ts.getAttribute(CharTermAttribute.class);
                pq.add(new Term(field, ta.toString()));
            }
            return pq;
        }
        return null;
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
}
