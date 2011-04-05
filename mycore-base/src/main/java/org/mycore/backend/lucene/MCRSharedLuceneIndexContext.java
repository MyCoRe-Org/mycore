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
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

import org.apache.log4j.Logger;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;

class MCRSharedLuceneIndexContext {
    Directory indexDir;

    IndexReader reader;

    IndexSearcher searcher;

    String ID;

    ReentrantReadWriteLock indexLock;

    Logger LOGGER = Logger.getLogger(MCRSharedLuceneIndexContext.class);

    public MCRSharedLuceneIndexContext(Directory indexDir, String ID) throws CorruptIndexException, IOException {
        this.indexDir = indexDir;
        this.ID = ID;
        indexLock = new ReentrantReadWriteLock();
        initReaderIfNeeded();
    }

    private void initReaderIfNeeded() throws CorruptIndexException, IOException {
        final int holdCount = indexLock.getReadHoldCount();
        try {
            if (reader == null && searcher == null) {
                for (int i = 0; i < holdCount; i++) {
                    indexLock.readLock().unlock();
                }
                indexLock.writeLock().lock();
                reader = IndexReader.open(indexDir, true);
                searcher = new IndexSearcher(reader);
                for (int i = 0; i < holdCount; i++) {
                    indexLock.readLock().lock();
                }
                indexLock.writeLock().unlock();
            } else {
                if (!reader.isCurrent()) {
                    IndexReader newReader = reader.reopen();
                    if (newReader != reader) {
                        LOGGER.info("new Searcher for index: " + ID);
                        for (int i = 0; i < holdCount; i++) {
                            indexLock.readLock().unlock();
                        }
                        indexLock.writeLock().lock();
                        reader.close();
                        searcher.close();
                        reader = newReader;
                        searcher = new IndexSearcher(reader);
                        for (int i = 0; i < holdCount; i++) {
                            indexLock.readLock().lock();
                        }
                        indexLock.writeLock().unlock();
                    }
                }
            }
        } finally {
            if (indexLock.isWriteLockedByCurrentThread()) {
                indexLock.writeLock().unlock();
            }
        }
    }

    public IndexReader getReader() throws CorruptIndexException, IOException {
        initReaderIfNeeded();
        return reader;
    }

    public IndexSearcher getSearcher() throws CorruptIndexException, IOException {
        initReaderIfNeeded();
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
            if (null != reader) {
                reader.close();
            }
            if (null != searcher) {
                searcher.close();
            }
        } catch (IOException e1) {
            LOGGER.warn("Error while closing indexreader " + toString(), e1);
        }
    }

    public ReadLock getIndexReadLock() {
        return indexLock.readLock();
    }
}
