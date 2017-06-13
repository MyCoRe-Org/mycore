/**
 * 
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
