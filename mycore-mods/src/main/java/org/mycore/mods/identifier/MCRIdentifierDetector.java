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

package org.mycore.mods.identifier;

import java.util.Map;
import java.util.Optional;

/**
 * Identifies identifiers in specific sources. E.g. T=URL could Detect the <b>GND <i>118948032</i></b> in a URL like <a href="http://d-nb.info/gnd/118948032">http://d-nb.info/gnd/118948032</a>.
 */
public interface MCRIdentifierDetector<T> {
    /**
     * @param resolvable some thing that can be resolved to a unique identifier
     * @return a {@link java.util.Map.Entry} with the identifier type as key and the identifier as value. The Optional can be empty if no identifier can be detected or if a error occurs.
     */
    Optional<Map.Entry<String, String>> detect(T resolvable);
}
