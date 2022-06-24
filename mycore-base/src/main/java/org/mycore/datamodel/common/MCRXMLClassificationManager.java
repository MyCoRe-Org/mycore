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

package org.mycore.datamodel.common;

import java.io.IOException;

import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.classifications2.MCRCategoryID;

/**
 * Interface for Classification Storage Manager
 * @author Tobias Lenhardt [Hammer1279]
 */
public interface MCRXMLClassificationManager {

    /**
     * Load a Classification from the Store
     * @param mcrid ID of the Category
     * @return MCRContent
     */
    default MCRContent retrieveContent(MCRCategoryID mcrid) throws IOException {
        return retrieveContent(mcrid, null);
    }

    /**
     * Load a Classification from the Store
     * @param mcrid ID of the Category
     * @param revision Revision of the Category
     * @return MCRContent
     */
    MCRContent retrieveContent(MCRCategoryID mcrid, String revision) throws IOException;

}
