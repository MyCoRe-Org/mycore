package org.mycore.common;

import java.util.Map;
import java.util.concurrent.Callable;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRExpandedObject;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * <p>Singleton class to manage the expansion of MCRObjects into MCRExpandedObjects. It uses
 * {@link MCRExpandedObjectCache} to store expanded objects on the disk. If objects or related objects are updated, the
 * {@link MCRExpandedObjectCacheEventHandler} will clear the cache for the affected objects.
 * The objects are expanded using the {@link MCRObjectExpander} interface, which is implemented by classes that know how
 * to gather the necessary information from the database and other resources to create a complete representation of the
 * object.
 * <p>The implementation of the {@link MCRObjectExpander} is defined via mycore properties. The default implementation
 * is defined by the key "Default" and can be overridden by other keys according to the type of the object.
 * Example:
 * <pre>
 *     MCR.ObjectExpander.Impl.Default = org.mycore.common.MCRBasicObjectExpander
 *     MCR.ObjectExpander.Impl.mods = org.mycore.mods.MCRMODSExpander
 * </pre>
 */
public final class MCRExpandedObjectManager {

    public static final String DEFAULT_IMPLEMENTATION_KEY = "Default";
    public static final String MCR_OBJECT_EXPANDER_PROPERTY_PREFIX = "MCR.ObjectExpander.Impl.";

    private final Map<String, Callable<MCRObjectExpander>> expanderMap;

    private MCRExpandedObjectManager() {
        expanderMap = MCRConfiguration2.getInstances(MCRObjectExpander.class, MCR_OBJECT_EXPANDER_PROPERTY_PREFIX);
    }

    /**
     * Returns the singleton instance of the MCRExpandedObjectManager.
     * Uses double-checked locking for lazy initialization and thread safety.
     *
     * @return The singleton instance.
     */
    public static MCRExpandedObjectManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Retrieves the expanded representation of the given MCRObject.
     * It first checks the {@link MCRExpandedObjectCache}. If the expanded object is not found or is stale,
     * it calls the appropriate {@link MCRObjectExpander} to generate it, caches the result, and returns it.
     *
     * @param object The MCRObject to expand.
     * @return The MCRExpandedObject representing the expanded state of the input object.
     * @throws MCRException if an error occurs during the expansion process.
     */
    public MCRExpandedObject getExpandedObject(MCRObject object) {
        return MCRExpandedObjectCache.getInstance().getExpandedObject(object.getId(), () -> expandObject(object));
    }

    /**
     * Selects the appropriate {@link MCRObjectExpander} based on the object's type ID and calls its expand method.
     * If no type-specific expander is configured, the default expander is used.
     *
     * @param object The MCRObject to expand.
     * @return The newly generated MCRExpandedObject.
     * @throws MCRException if no suitable expander is found or if the expansion fails.
     */
    private MCRExpandedObject expandObject(MCRObject object) {
        String type = object.getId().getTypeId();
        Callable<MCRObjectExpander> expanderConstructor =
            expanderMap.getOrDefault(type, expanderMap.get(DEFAULT_IMPLEMENTATION_KEY));

        try {
            MCRObjectExpander expander = expanderConstructor.call();
            return expander.expand(object);
        } catch (Exception e) {
            throw new MCRException("Error while expanding object " + object.getId(), e);
        }
    }

    private static final class InstanceHolder {
        private static final MCRExpandedObjectManager INSTANCE = new MCRExpandedObjectManager();
    }

}
