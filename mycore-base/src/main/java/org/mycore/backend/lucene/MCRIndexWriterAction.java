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

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

class MCRIndexWriterAction implements Runnable {
    private MCRIndexWriteExecutor executor;

    private Document doc;

    private Analyzer analyzer;

    private boolean add = false;

    private boolean delete = false;

    private boolean optimize = false;

    private Term deleteTerm;

    private RAMDirectory ramDir;

    private static Logger LOGGER = Logger.getLogger(MCRIndexWriterAction.class);

    private MCRIndexWriterAction(MCRIndexWriteExecutor executor) {
        this.executor = executor;
    }

    public static MCRIndexWriterAction addAction(MCRIndexWriteExecutor executor, Document doc, Analyzer analyzer) {
        MCRIndexWriterAction e = new MCRIndexWriterAction(executor);
        e.doc = doc;
        e.analyzer = analyzer;
        e.add = true;
        return e;
    }

    public static MCRIndexWriterAction removeAction(MCRIndexWriteExecutor executor, Term deleteTerm) {
        MCRIndexWriterAction e = new MCRIndexWriterAction(executor);
        e.delete = true;
        e.deleteTerm = deleteTerm;
        return e;
    }

    public static MCRIndexWriterAction optimizeAction(MCRIndexWriteExecutor executor) {
        MCRIndexWriterAction e = new MCRIndexWriterAction(executor);
        e.optimize = true;
        return e;
    }

    public static MCRIndexWriterAction addRamDir(MCRIndexWriteExecutor executor, RAMDirectory ramDir) {
        MCRIndexWriterAction e = new MCRIndexWriterAction(executor);
        e.ramDir = ramDir;
        return e;
    }

    public void run() {
        try {
            if (delete) {
                deleteDocument();
            } else if (add) {
                addDocument();
            } else if (optimize) {
                optimizeIndex();
            } else {
                addDirectory();
            }
        } catch (Exception e) {
            LOGGER.error("Error while writing Lucene Index ", e);
        }
    }

    private void addDocument() throws IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("add Document:" + toString());
        }
        executor.getIndexWriter().addDocument(doc, analyzer);
        LOGGER.debug("adding done.");
    }

    private void deleteDocument() throws IOException {
        LOGGER.debug("delete Document:" + toString());
        executor.getIndexWriter().deleteDocuments(deleteTerm);
    }

    private void optimizeIndex() throws IOException {
        LOGGER.info("optimize Index:" + toString());
        executor.getIndexWriter().optimize();
        LOGGER.info("Optimizing done.");
    }

    private void addDirectory() throws IOException {
        LOGGER.info("add Directory");
        executor.getIndexWriter().addIndexesNoOptimize(new Directory[] { ramDir });
        LOGGER.info("Adding done.");
    }

    @Override
    public String toString() {
        if (doc != null) {
            return doc.toString();
        }
        if (deleteTerm != null) {
            return deleteTerm.toString();
        }
        return "empty IndexWriterAction";
    }
}