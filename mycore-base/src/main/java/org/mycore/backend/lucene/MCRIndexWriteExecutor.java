/*
 * $Id$
 * $Revision: 5697 $ $Date: 14.10.2009 $
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.mycore.common.MCRConfiguration;

class MCRIndexWriteExecutor extends ThreadPoolExecutor {
    boolean modifierClosed, firstJob, closeModifierEarly;

    private IndexWriter indexWriter;

    private FSDirectory indexDir;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final MCRDelayedIndexWriterCloser delayedCloser = new MCRDelayedIndexWriterCloser(this);

    private ScheduledFuture<?> delayedFuture;

    private int maxIndexWriteActions;

    private static Logger LOGGER = Logger.getLogger(MCRIndexWriteExecutor.class);

    private ReadWriteLock IndexCloserLock = new ReentrantReadWriteLock(true);

    private ThreadLocal<Lock> writeAccess = new ThreadLocal<Lock>() {

        @Override
        protected Lock initialValue() {
            return IndexCloserLock.readLock();
        }
    };

    public MCRIndexWriteExecutor(BlockingQueue<Runnable> workQueue, FSDirectory indexDir) {
        // single thread mode
        super(1, 1, 0, TimeUnit.SECONDS, workQueue);
        this.indexDir = indexDir;
        modifierClosed = true;
        firstJob = true;
        closeModifierEarly = MCRConfiguration.instance().getBoolean("MCR.Lucene.closeModifierEarly", false);
        maxIndexWriteActions = MCRConfiguration.instance().getInt("MCR.Lucene.maxIndexWriteActions", 500);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        //allow to close the IndexWriter
        writeAccess.get().unlock();
        if (firstJob) {
            firstJob = false;
        }
        if (closeModifierEarly || getCompletedTaskCount() % maxIndexWriteActions == 0) {
            closeIndexWriter();
        } else {
            cancelDelayedIndexCloser();
            try {
                delayedFuture = scheduler.schedule(delayedCloser, 2, TimeUnit.SECONDS);
            } catch (RejectedExecutionException e) {
                LOGGER.warn("Cannot schedule delayed IndexWriter closer. Closing IndexWriter now.");
                closeIndexWriter();
            }
        }
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        //do not close IndexWriter while IndexWriterActions is processed
        cancelDelayedIndexCloser();
        writeAccess.get().lock();
        if (modifierClosed) {
            openIndexWriter();
        }
        super.beforeExecute(t, r);
    }

    private void cancelDelayedIndexCloser() {
        if (delayedFuture != null && !delayedFuture.isDone()) {
            delayedFuture.cancel(false);
        }
    }

    @Override
    public void shutdown() {
        cancelDelayedIndexCloser();
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(60 * 60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.warn("Error while closing DelayedIndexWriterCloser", e);
        }
        super.shutdown();
        closeIndexWriter();
    }

    private void openIndexWriter() {
        try {
            LOGGER.debug("Opening Lucene index for writing.");
            if (indexWriter == null) {
                indexWriter = getLuceneWriter(indexDir, firstJob);
            }
        } catch (Exception e) {
            LOGGER.warn("Error while reopening IndexWriter.", e);
        } finally {
            modifierClosed = false;
        }
    }

    void closeIndexWriter() {
        //TODO: check if indexWriter.commit() is sufficient here
        Lock writerLock = IndexCloserLock.writeLock();
        try {
            //do not allow IndexWriterAction being processed while closing IndexWriter
            writerLock.lock();
            if (indexWriter != null) {
                LOGGER.debug("Writing Lucene index changes to disk.");
                indexWriter.close();
            }
        } catch (IOException e) {
            LOGGER.warn("Error while closing IndexWriter.", e);
        } catch (IllegalStateException e) {
            LOGGER.debug("IndexWriter was allready closed.", e);
        } finally {
            modifierClosed = true;
            indexWriter = null;
            writerLock.unlock();
        }
    }

    private static IndexWriter getLuceneWriter(FSDirectory indexDir, boolean first) throws Exception {
        IndexWriter modifier;
        Analyzer analyzer = new GermanAnalyzer(Version.LUCENE_30);
        boolean create = false;
        // check if indexDir is empty before creating a new index
        if (first && indexDir.listAll().length == 0) {
            LOGGER.info("No Entries in Directory, initialize: " + indexDir);
            create = true;
        }
        modifier = new IndexWriter(indexDir, analyzer, create, MaxFieldLength.LIMITED);
        modifier.setMergeFactor(200);
        modifier.setMaxBufferedDocs(2000);
        return modifier;
    }

    public IndexWriter getIndexWriter() {
        return indexWriter;
    }

    @Override
    protected void finalize() {
        closeIndexWriter();
        super.finalize();
    }

}
