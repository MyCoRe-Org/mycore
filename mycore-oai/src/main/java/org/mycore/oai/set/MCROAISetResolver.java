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
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.mycore.oai.pmh.Set;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCROAISetResolver<K, T> {

    private String configPrefix;

    private String setId;

    private Collection<T> result;

    private Function<T, K> identifier;

    private Map<String, MCRSet> setMap;

    /**
     * Initializes the set handler with the configPrefix
     * (MCR.OAIDataProvider.MY_PROVIDER) and a setId
     * (MCR.OAIDataProvider.MY_PROVIDER.Sets.SET_ID).
     *
     * @param configPrefix the config prefix
     * @param setId the set id without any prefix
     */
    public void init(String configPrefix, String setId, Map<String, MCRSet> setMap, Collection<T> result,
        Function<T, K> identifier) {
        LogManager.getLogger("init: " + setId);
        this.configPrefix = configPrefix;
        this.setId = setId;
        this.setMap = setMap;
        this.result = result;
        this.identifier = identifier;
    }

    /** Returns a collection of Sets for the current SetSpec.
     * 
     * @param key is the key of the result
     * @return this implementation returns empty set, should be overwritten
     */
    public Collection<Set> getSets(K key) {
        return Collections.emptySet();
    }

    protected String getConfigPrefix() {
        return configPrefix;
    }

    protected String getSetId() {
        return setId;
    }

    protected Collection<T> getResult() {
        return result;
    }

    protected Function<T, K> getIdentifier() {
        return identifier;
    }

    protected Map<String, MCRSet> getSetMap() {
        return setMap;
    }

}
