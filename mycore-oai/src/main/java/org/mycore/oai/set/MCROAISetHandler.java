package org.mycore.oai.set;

import org.mycore.oai.pmh.Set;

/**
 * Base interface to handle mycore oai sets.
 *
 * @author Matthias Eichner
 *
 * @param <T>
 */
public interface MCROAISetHandler<T> {

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
     * Called before {@link #apply(Set, Object)} to check if the
     * given set should be added to the ListSets view.
     *
     * @return false if the given set should be added (the
     *           set is not filtered)
     */
    public default boolean filter(Set set) {
        return false;
    }

    public void apply(Set set, T t);

}
