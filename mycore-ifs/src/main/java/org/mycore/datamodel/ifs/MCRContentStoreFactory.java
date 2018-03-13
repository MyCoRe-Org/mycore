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

package org.mycore.datamodel.ifs;

import java.util.Hashtable;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;

/**
 * This class manages instances of MCRContentStore and MCRAudioVideoExtender and
 * provides methods to get these for a given Store ID or MCRFile instance. The
 * class is responsible for looking up, loading, instantiating and remembering
 * the implementations of MCRContentStore and MCRAudioVideoExtender that are
 * used in the system.
 * 
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRContentStoreFactory {
    private static final String CLASS_SUFFIX = ".Class";

    private static final String CONFIG_PREFIX = "MCR.IFS.ContentStore.";

    /** Hashtable StoreID to MCRContentStore instance */
    protected static final Hashtable<String, MCRContentStore> STORES = new Hashtable<>();

    /** Hashtable StoreID to Class that implements MCRAudioVideoExtender */
    protected static Hashtable<String, Class<? extends MCRAudioVideoExtender>> EXTENDER_CLASSES;

    /** The MCRContentStoreSelector implementation that will be used */
    protected static MCRContentStoreSelector STORE_SELECTOR;

    private static final Logger LOGGER = LogManager.getLogger(MCRContentStoreFactory.class);

    public static Map<String, MCRContentStore> getAvailableStores() {
        Map<String, String> properties = MCRConfiguration.instance().getPropertiesMap(CONFIG_PREFIX);
        for (Map.Entry<String, String> prop : properties.entrySet()) {
            String key = prop.getKey();
            if (key.endsWith(CLASS_SUFFIX)) {
                String storeID = key.replace(CONFIG_PREFIX, "").replace(CLASS_SUFFIX, "");
                initStore(storeID);
            }
        }
        return STORES;
    }

    /**
     * Returns the MCRContentStore instance that is configured for this StoreID.
     * The instance that is returned is configured by the property
     * <tt>MCR.IFS.ContentStore.&lt;StoreID&gt;.Class</tt> in mycore.properties.
     * 
     * @param storeID
     *            the non-null ID of the MCRContentStore implementation
     * @return the MCRContentStore instance that uses this storeID
     * @throws MCRConfigurationException
     *             if no MCRContentStore implementation is configured for this
     *             storeID
     */
    public static MCRContentStore getStore(String storeID) {
        if (storeID == null || storeID.length() == 0) {
            return null;
        }
        if (!STORES.containsKey(storeID)) {
            synchronized (STORES) {
                if (!STORES.containsKey(storeID)) {
                    initStore(storeID);
                }
            }
        }
        return STORES.get(storeID);
    }

    /**
     * Returns instance of a default content store.
     * 
     * Depends on the configured MCRContentStoreSelector which instance is "default".
     */
    public static MCRContentStore getDefaultStore() {
        return getStore(STORE_SELECTOR.getDefaultStore());
    }

    private static void initStore(String storeID) {
        try {
            String storeClass = CONFIG_PREFIX + storeID + CLASS_SUFFIX;
            LOGGER.debug("getting StoreClass: {}", storeClass);

            MCRContentStore s = MCRConfiguration.instance().getInstanceOf(storeClass);
            s.init(storeID);
            STORES.put(storeID, s);

        } catch (Exception ex) {
            String msg = "Could not load MCRContentStore with store ID = " + storeID;
            throw new MCRConfigurationException(msg, ex);
        }
    }

    /**
     * Returns the MCRContentStore instance that should be used to store the
     * content of the given file. The configured MCRContentStoreSelector is used
     * to make this decision.
     * 
     * @see MCRContentStoreSelector
     * @see MCRContentStore
     */
    public static MCRContentStore selectStore(MCRFile file) {
        if (STORE_SELECTOR == null) {
            initStoreSelector();
        }

        String store = STORE_SELECTOR.selectStore(file);

        return getStore(store);
    }

    private static void initStoreSelector() {
        String property = "MCR.IFS.ContentStoreSelector.Class";
        STORE_SELECTOR = MCRConfiguration.instance().getInstanceOf(property);
    }

    /**
     * Returns the Class instance that implements the MCRAudioVideoExtender for
     * the MCRContentStore with the given ID. That class is configured by the
     * property <tt>MCR.IFS.AVExtender.&lt;StoreID&gt;.Class</tt> in
     * mycore.properties.
     * 
     * @param storeID
     *            the non-null StoreID of the MCRContentStore
     * @return the Class that implements MCRAudioVideoExtender for the StoreID
     *         given, or null
     * @throws MCRConfigurationException
     *             if the MCRAudioVideoExtender implementation class could not
     *             be loaded
     */
    protected static Class<? extends MCRAudioVideoExtender> getExtenderClass(String storeID) {
        if (storeID == null || storeID.length() == 0) {
            return null;
        }
        if (EXTENDER_CLASSES == null) {
            EXTENDER_CLASSES = new Hashtable<>();
        }

        return EXTENDER_CLASSES.computeIfAbsent(storeID, key -> {
            String storeClass = "MCR.IFS.AVExtender." + key + CLASS_SUFFIX;
            return MCRConfiguration.instance().<MCRAudioVideoExtender>getClass(storeClass, null);
        });
    }

    /**
     * Returns true if the MCRContentStore with the given StoreID provides an
     * MCRAudioVideoExtender implementation, false otherwise. The
     * MCRAudioVideoExtender for a certain MCRContentStore is configured by the
     * property <tt>MCR.IFS.AVExtender.&lt;StoreID&gt;.Class</tt> in
     * mycore.properties.
     * 
     * @param storeID
     *            the non-null StoreID of the MCRContentStore
     * @return the MCRAudioVideoExtender for the StoreID given, or null
     * @throws MCRConfigurationException
     *             if the MCRAudioVideoExtender implementation class could not
     *             be loaded
     */
    static boolean providesAudioVideoExtender(String storeID) {
        return getExtenderClass(storeID) != null;
    }

    /**
     * If the MCRContentStore of the MCRFile given provides an
     * MCRAudioVideoExtender implementation, this method creates and initializes
     * the MCRAudioVideoExtender instance for the MCRFile. The instance that is
     * returned is configured by the property
     * <tt>MCR.IFS.AVExtender.&lt;StoreID&gt;.Class</tt> in mycore.properties.
     * 
     * @param file
     *            the non-null MCRFile that should get an MCRAudioVideoExtender
     * @return the MCRAudioVideoExtender for the MCRFile given, or null
     * @throws MCRConfigurationException
     *             if the MCRAudioVideoExtender implementation class could not
     *             be loaded
     */
    static MCRAudioVideoExtender buildExtender(MCRFile file) {
        if (file == null || !providesAudioVideoExtender(file.getStoreID())) {
            return null;
        }

        Class<? extends MCRAudioVideoExtender> cl = getExtenderClass(file.getStoreID());

        try {
            MCRAudioVideoExtender ext = cl.getDeclaredConstructor().newInstance();
            ext.init(file);
            return ext;
        } catch (Exception exc) {
            if (exc instanceof MCRException) {
                throw (MCRException) exc;
            }

            String msg = "Could not build MCRAudioVideoExtender instance";
            throw new MCRConfigurationException(msg, exc);
        }
    }
}
