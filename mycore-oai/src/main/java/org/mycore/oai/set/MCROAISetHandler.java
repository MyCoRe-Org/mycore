package org.mycore.oai.set;

import java.util.Collection;
import java.util.Map;

/**
 * Base interface to handle mycore oai sets.
 *
 * @author Matthias Eichner
 *
 * @param <Q> set type
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
    public void init(String configPrefix, String setId);

    /**
     * Called before {@link #apply(MCRSet, Object)} to check if the
     * given set should be added to the ListSets view.
     *
     * @return false if the given set should be added (the
     *           set is not filtered)
     */
    public default boolean filter(MCRSet set) {
        return false;
    }
    
    public Map<String, MCRSet> getSetMap();

    public void apply(MCRSet set, Q q);
    
    public MCROAISetResolver<K, R> getSetResolver(Collection<R> result);

}
