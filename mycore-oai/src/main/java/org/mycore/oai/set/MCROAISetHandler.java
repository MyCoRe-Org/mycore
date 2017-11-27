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

package org.mycore.oai.set;

import java.util.Collection;
import java.util.Map;

/**
 * Base interface to handle mycore oai sets.
 *
 * @author Matthias Eichner
 *
 * @param <Q> set type
 * @param <R> Result collection type
 * @param <K> Result collection key type
 */
public interface MCROAISetHandler<Q, R, K> {

    /**
     * Initializes the set handler with the configPrefix
     * (MCR.OAIDataProvider.MY_PROVIDER) and a setId
     * (MCR.OAIDataProvider.MY_PROVIDER.Sets.SET_ID).
     *
     * @param configPrefix the config prefix
     * @param setId the set id without any prefix
     */
    void init(String configPrefix, String setId);

    /**
     * Called before {@link #apply(MCRSet, Object)} to check if the
     * given set should be added to the ListSets view.
     *
     * @return false if the given set should be added (the
     *           set is not filtered)
     */
    default boolean filter(MCRSet set) {
        return false;
    }

    Map<String, MCRSet> getSetMap();

    void apply(MCRSet set, Q q);

    MCROAISetResolver<K, R> getSetResolver(Collection<R> result);

}
