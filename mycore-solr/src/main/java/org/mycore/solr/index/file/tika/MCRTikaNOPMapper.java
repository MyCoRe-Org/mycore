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

package org.mycore.solr.index.file.tika;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.solr.common.SolrInputDocument;

import com.fasterxml.jackson.core.TreeNode;

/**
 * A NOP (no operation) implementation of the MCRTikaMapper interface.
 * This implementation does nothing when the map method is called.
 */
public class MCRTikaNOPMapper implements MCRTikaMapper {

    @Override
    public void map(String key, TreeNode element, SolrInputDocument document, Path filePath,
                    BasicFileAttributes attributes) throws MCRTikaMappingException {
        // do nothing
    }

}
