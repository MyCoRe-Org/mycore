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

package org.mycore.oai;

import java.util.List;
import java.util.Optional;

import org.mycore.oai.pmh.Header;

/**
 * The result of a searcher query.
 * 
 * @author Matthias Eichner
 */
public interface MCROAIResult {

    /**
     * Returns a list of mycore object identifiers.
     * 
     * @return list of mycore object identifiers
     */
    List<Header> list();

    /**
     * Number of all hits
     * 
     * @return number of hits
     */
    int getNumHits();

    /**
     * @return the next cursor
     */
    Optional<String> nextCursor();

}
