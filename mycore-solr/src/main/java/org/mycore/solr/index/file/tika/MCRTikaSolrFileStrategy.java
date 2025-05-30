/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import org.mycore.solr.index.strategy.MCRSolrFileStrategy;

/**
 * The MCRTikaSolrFileStrategy is a strategy to check if a file should be sent to solr. (which is always false, because
 * the file gets parsed by external tika server and not by solr itself)
 */
public class MCRTikaSolrFileStrategy implements MCRSolrFileStrategy {

    @Override
    public boolean check(Path file, BasicFileAttributes attrs) {
        return false;
    }
}
