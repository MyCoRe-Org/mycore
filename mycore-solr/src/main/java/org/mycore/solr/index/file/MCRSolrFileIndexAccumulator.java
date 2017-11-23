/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.solr.index.file;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.solr.common.SolrInputDocument;

/**
 * This interface is used to accumulate information of a file to a solr document.
 * Add instances of this interface to the property <code>MCR.Module-solr.Indexer.File.AccumulatorList</code>
 */
public interface MCRSolrFileIndexAccumulator {
    /**
     * Adds additional information to a File.
     * @param document which holds the information
     * @param filePath to the file in a derivate
     * @param attributes of the file in a derivate
     */
    void accumulate(SolrInputDocument document, Path filePath, BasicFileAttributes attributes) throws IOException;
}
