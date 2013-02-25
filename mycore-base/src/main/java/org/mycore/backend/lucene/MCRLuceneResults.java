package org.mycore.backend.lucene;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.AlreadyClosedException;
import org.mycore.common.MCRException;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRResults;

/**
 * This class is a special Lucene version of MCRResults
 * It is read only but fast on large result set as it is filled lazy. 
 * @author Thomas Scheffler (yagee)
 */
class MCRLuceneResults extends MCRResults {

    private TopFieldDocs topDocs;

    private Collection<MCRFieldDef> addableFields;

    private static final DecimalFormat df = new DecimalFormat("0.00000000000");

    private boolean loadComplete = false;

    private Sort sortFields;

    private Query query;

    private int maxResults;

    private int initialized;

    private MCRSharedLuceneIndexContext sharedIndexContext;

    //    private IndexSearcher indexSearcher;

    private static Logger LOGGER = Logger.getLogger(MCRLuceneResults.class);

    public MCRLuceneResults(MCRSharedLuceneIndexContext sharedIndexContext, Collection<MCRFieldDef> addableFields, Sort sortFields, Query luceneQuery,
        int maxResults) throws CorruptIndexException, IOException {
        super();
        this.addableFields = addableFields;
        this.sharedIndexContext = sharedIndexContext;
        this.sortFields = sortFields;
        query = luceneQuery;
        this.maxResults = maxResults;
        reQuery();
    }

    private void fillHitList() {
        topDocs.totalHits = topDocs.scoreDocs.length;
        initialized = 0;
        super.hits = new ArrayList<MCRHit>(topDocs.totalHits);
        super.hits.addAll(Collections.nCopies(topDocs.totalHits, (MCRHit) null));
        setSorted(true);
    }

    private void reQuery() throws IOException {
        TopFieldCollector collector = TopFieldCollector.create(sortFields, maxResults, false, true, false, false);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Query: " + query);
        }
        sharedIndexContext.getIndexReadLock().lock();
        try {
            sharedIndexContext.getSearcher().search(query, collector);
        } finally {
            sharedIndexContext.getIndexReadLock().unlock();
        }
        //Lucene 2.4.1 has a bug: be sure to call collector.topDocs() just once
        //see http://issues.apache.org/jira/browse/LUCENE-942
        topDocs = (TopFieldDocs) collector.topDocs();
        fillHitList();
        setSorted(true);
    }

    @Override
    public boolean isReadonly() {
        return true;
    }

    @Override
    public void addHit(MCRHit hit) {
        throw new UnsupportedOperationException("MCRResults are read only");
    }

    @Override
    protected int merge(org.jdom2.Document doc, String hostAlias) {
        throw new UnsupportedOperationException("MCRResults are read only");
    }

    @Override
    protected MCRHit getHit(String key) {
        if (!loadComplete) {
            fetchAllHits();
        }
        return super.getHit(key);
    }

    @Override
    public void fetchAllHits() {
        sharedIndexContext.getIndexReadLock().lock();
        try {
            IndexSearcher indexSearcher;
            try {
                indexSearcher = sharedIndexContext.getSearcher();
            } catch (IOException e) {
                throw new MCRException(e);
            }
            int numHits = getNumHits();
            for (int i = 0; i < numHits; i++) {
                if (super.getHit(i) == null) {
                    inititializeTopDoc(i, indexSearcher);
                }
            }
        } finally {
            sharedIndexContext.getIndexReadLock().unlock();
        }
    }

    @Override
    public MCRHit getHit(int i) {
        if (i < 0 || i > getNumHits()) {
            return null;
        }
        MCRHit hit = super.getHit(i);
        if (hit == null) {
            sharedIndexContext.getIndexReadLock().lock();
            try {
                inititializeTopDoc(i, sharedIndexContext.getSearcher());
            } catch (IOException e) {
                throw new MCRException(e);
            } finally {
                sharedIndexContext.getIndexReadLock().unlock();
            }
            hit = super.getHit(i);
        }
        return hit;
    }

    private boolean inititializeTopDoc(int i, IndexSearcher indexSearcher) {
        boolean reQuery = false;
        //initialize
        MCRHit hit;
        try {
            hit = getMCRHit(topDocs.scoreDocs[i], indexSearcher);
        } catch (Exception e) {
            if (topDocs.scoreDocs.length <= i) {
                throw new MCRException("TopDocs is not initialized.", e);
            }
            if (e instanceof AlreadyClosedException) {
                LOGGER.warn("Invalid IndexReader for fetching Lucene document: " + topDocs.scoreDocs[i].doc + "\nRequery Lucene index.");
            } else {
                LOGGER.warn("Error while fetching Lucene document: " + topDocs.scoreDocs[i].doc + "\nRequery Lucene index.", e);
            }
            try {
                reQuery();
                reQuery = true;
                if (i < topDocs.scoreDocs.length) {
                    hit = getMCRHit(topDocs.scoreDocs[i], indexSearcher);
                } else {
                    LOGGER.warn("There is no such result anymore: " + i);
                    return reQuery;
                }
            } catch (IOException ioe) {
                throw new MCRException("Error while requerying Lucene index.", ioe);
            }
        }
        super.hits.set(i, hit);
        initialized++;
        if (initialized == topDocs.scoreDocs.length) {
            loadComplete = true;
        }
        MCRHit oldHit = super.map.get(hit.getKey());
        if (oldHit != null) {
            oldHit.merge(hit);
        } else {
            super.map.put(hit.getKey(), hit);
        }
        return reQuery;
    }

    private MCRHit getMCRHit(ScoreDoc scoreDoc, IndexSearcher indexSearcher) throws CorruptIndexException, IOException {
        org.apache.lucene.document.Document doc = indexSearcher.doc(scoreDoc.doc);

        String id = doc.get("returnid");
        MCRHit hit = new MCRHit(id);

        for (MCRFieldDef fd : addableFields) {
            String[] values = doc.getValues(fd.getName());
            for (String value : values) {
                MCRFieldValue fv = new MCRFieldValue(fd.getName(), value);
                hit.addMetaData(fv);
            }
        }

        String score = df.format(scoreDoc.score);
        addSortDataToHit(doc, hit, score, topDocs.fields);
        return hit;
    }

    private static void addSortDataToHit(org.apache.lucene.document.Document doc, MCRHit hit, String score, SortField[] sortFields) {
        for (SortField sortField : sortFields) {
            if (SortField.FIELD_SCORE == sortField || sortField.getField() == null) {
                if (score != null) {
                    hit.addSortData(new MCRFieldValue("score", score));
                }
            } else {
                String fieldName = sortField.getField();
                if (fieldName.endsWith(MCRLuceneSearcher.getSortableSuffix())) {
                    fieldName = fieldName.substring(0, fieldName.length() - MCRLuceneSearcher.getSortableSuffix().length());
                }

                String values[] = doc.getValues(fieldName);
                for (String value : values) {
                    MCRFieldValue fv = new MCRFieldValue(fieldName, value);
                    hit.addSortData(fv);
                }
            }
        }
    }

    @Override
    public int getNumHits() {
        return topDocs.totalHits;
    }

    @Override
    public void cutResults(int maxResults) {
        while (hits.size() > maxResults && maxResults > 0) {
            MCRHit hit = hits.remove(hits.size() - 1);
            topDocs.totalHits--;
            if (hit != null) {
                map.remove(hit.getKey());
            }
        }
    }

    @Override
    public Iterator<MCRHit> iterator() {
        return new Iterator<MCRHit>() {
            int i = 0;

            public boolean hasNext() {
                return i < topDocs.totalHits;
            }

            public MCRHit next() {
                MCRHit hit = getHit(i);
                i++;
                return hit;
            }

            public void remove() {
                throw new UnsupportedOperationException("MCRResults are read only");
            }

        };
    }
}
