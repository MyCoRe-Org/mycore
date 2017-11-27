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

package org.mycore.solr.index.strategy;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.mycore.common.config.MCRConfiguration;

/**
 * @author Matthias Eichner
 * @author Thomas Scheffler (yagee)
 */
public class MCRSolrIndexStrategyManager {

    private static final MCRSolrFileStrategy FILE_STRATEGY;

    static {
        FILE_STRATEGY = MCRConfiguration.instance().getInstanceOf(
            CONFIG_PREFIX + "FileIndexStrategy", null);
    }

    public static boolean checkFile(Path file, BasicFileAttributes attrs) {
        return FILE_STRATEGY.check(file, attrs);
    }

}
