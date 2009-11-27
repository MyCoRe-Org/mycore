/*
 * $Id$
 * $Revision: 5697 $ $Date: 08.10.2009 $
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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRSortBy;

class MCRSharedLuceneIndexContext {
    private static final class NoOpCollector extends HitCollector {
        @Override
        public void collect(int arg0, float arg1) {
        }
    }

    Directory indexDir;

    IndexReader reader;

    IndexSearcher searcher;

    String ID;

    Logger LOGGER = Logger.getLogger(MCRSharedLuceneIndexContext.class);

    ScheduledThreadPoolExecutor executorService;

    private String index;

    public MCRSharedLuceneIndexContext(Directory indexDir, String ID) throws CorruptIndexException, IOException {
        this.indexDir = indexDir;
        this.ID = ID;
        this.index = MCRConfiguration.instance().getString("MCR.Searcher." + ID + "." + "Index");
        this.executorService = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            public Thread newThread(Runnable r) {
                return new Thread("Index." + index + ".Refresher");
            }
        });
        RefreshIndexSearcher refreshIndexSearcher = new RefreshIndexSearcher(this);
        refreshIndexSearcher.run();
    }

    public IndexReader getReader() throws CorruptIndexException, IOException {
        return reader;
    }

    public IndexSearcher getSearcher() throws CorruptIndexException, IOException {
        return searcher;
    }

    public boolean isValid(IndexReader reader) {
        return this.reader == reader;
    }

    public boolean isValid(IndexSearcher searcher) {
        return this.searcher == searcher;
    }

    public void close() {
        try {
            if (null != reader)
                reader.close();
            if (null != searcher)
                searcher.close();
        } catch (IOException e1) {
            LOGGER.warn("Error while closing indexreader " + toString(), e1);
        }
    }

    public String getIndex() {
        return index;
    }

    /**
     * refreshes IndexSearcher if needed with a warm up for optimal query performance.
     * @author Thomas Scheffler (yagee)
     *
     */
    private static class RefreshIndexSearcher implements Runnable {
        private MCRSharedLuceneIndexContext context;

        public RefreshIndexSearcher(MCRSharedLuceneIndexContext context) {
            this.context = context;
        }

        public void run() {
            try {
                Long start = System.currentTimeMillis();
                initReaderIfNeeded();
                Long end = System.currentTimeMillis();
                long nextCheck = Math.max((end - start), 60 * 1000);
                if (context.LOGGER.isDebugEnabled()) {
                    context.LOGGER.debug("Scheduling next index refresh in " + nextCheck + " ms.");
                }
                context.executorService.schedule(this, nextCheck, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw new MCRException("Error while opening Lucene index.", e);
            }
        }

        private void initReaderIfNeeded() throws CorruptIndexException, IOException {
            if (context.reader == null && context.searcher == null) {
                context.reader = IndexReader.open(context.indexDir);
                context.searcher = new IndexSearcher(context.reader);
                warmUpSearcher(context.searcher);
            } else {
                if (!context.reader.isCurrent()) {
                    IndexReader newReader = context.reader.reopen();
                    if (newReader != context.reader) {
                        context.LOGGER.info("new Searcher for index: " + context.index);
                        IndexSearcher newSearcher = new IndexSearcher(newReader);
                        warmUpSearcher(newSearcher);
                        context.reader.close();
                        context.searcher.close();
                        context.reader = newReader;
                        context.searcher = newSearcher;
                    }
                }
            }
        }

        private void warmUpSearcher(IndexSearcher newSearcher) throws IOException {
            long start = System.currentTimeMillis();
            context.LOGGER.debug("Warming up IndexSearcher for index " + context.ID);
            List<MCRFieldDef> fieldDefs = MCRFieldDef.getFieldDefs(context.getIndex());
            HashSet<String> fieldNames=new HashSet<String>();
            @SuppressWarnings("unchecked")
            Collection<String> fldNames = newSearcher.getIndexReader().getFieldNames(IndexReader.FieldOption.ALL);
            fieldNames.addAll(fldNames);
            for (MCRFieldDef fieldDef : fieldDefs) {
                if (!fieldNames.contains(fieldDef.getName()))
                    continue;
                if (fieldDef.isSortable()) {
                    Query query;
                    if (MCRLuceneSearcher.isTokenized(fieldDef)) {
                        query = new TermQuery(new Term(fieldDef.getName() + MCRLuceneSearcher.getSortableSuffix()));
                    }
                    query = new TermQuery(new Term(fieldDef.getName()));
                    Sort sortFields = MCRLuceneSearcher.buildSortFields(Collections.nCopies(1, new MCRSortBy(fieldDef, true)));
                    if (context.LOGGER.isDebugEnabled()){
                        for (SortField sortField : sortFields.getSort()) {
                            String name = (SortField.FIELD_SCORE == sortField ? "score" : sortField.getField());
                            context.LOGGER.debug("Sort by: " + name + (sortField.getReverse() ? " descending" : " accending"));
                        }
                    }
                    newSearcher.search(query, null, newSearcher.maxDoc(), sortFields);
                } else {
                    Query query = new TermQuery(new Term(fieldDef.getName()));
                    newSearcher.search(query, new NoOpCollector());
                }
            }
            if (context.LOGGER.isDebugEnabled()) {
                context.LOGGER.debug("Warming up IndexSearcher " + context.ID + " took " + (System.currentTimeMillis() - start) + " ms.");
            }
        }
    }
}