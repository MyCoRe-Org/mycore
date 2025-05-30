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

package org.mycore.solr;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

/**
 * @author shermann
 * @author Thomas Scheffler (yagee)
 * @author Matthias Eichner
 * @author Jens Kupferschmidt
 */
public final class MCRSolrCoreManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private static Map<String, MCRSolrCore> coreMap;

    static {
        try {
            coreMap = Collections.synchronizedMap(loadCoresFromProperties());
        } catch (Exception e) {
            LOGGER.error("Exception creating solr client object", e);
        }
    }

    private MCRSolrCoreManager() {
    }

    /**
     * MCR.Solr.Core.Main.Name=cmo
     * MCR.Solr.Core.Classfication.Name=cmo-classification
     * @return a map of all cores defined in the properties.
     */
    private static Map<String, MCRSolrCore> loadCoresFromProperties() {
        return MCRConfiguration2
            .getPropertiesMap()
            .keySet()
            .stream()
            .filter(p -> p.startsWith(MCRSolrConstants.SOLR_CORE_PREFIX))
            .map(cp -> cp.substring(MCRSolrConstants.SOLR_CORE_PREFIX.length()))
            .map(cp -> {
                int indexOfDot = cp.indexOf('.');
                return indexOfDot != -1 ? cp.substring(0, indexOfDot) : cp;
            })
            .distinct()
            .collect(Collectors.toMap(coreID -> coreID, MCRSolrCoreManager::initializeSolrCore));
    }

    private static MCRSolrCore initializeSolrCore(String coreID) {
        final String coreNameKey = MCRSolrConstants.SOLR_CORE_PREFIX + coreID + MCRSolrConstants.SOLR_CORE_NAME_SUFFIX;
        final String coreServerKey = MCRSolrConstants.SOLR_CORE_PREFIX + coreID
            + MCRSolrConstants.SOLR_CORE_SERVER_SUFFIX;

        final String configSetTemplateKey = MCRSolrConstants.SOLR_CORE_PREFIX + coreID
            + MCRSolrConstants.SOLR_CORE_CONFIGSET_TEMPLATE_SUFFIX;

        final String shardCountKey = MCRSolrConstants.SOLR_CORE_PREFIX + coreID
            + MCRSolrConstants.SOLR_CORE_SHARD_COUNT_SUFFIX;

        String coreName = MCRConfiguration2.getString(coreNameKey)
            .orElseThrow(() -> new MCRConfigurationException("Missing property " + coreNameKey));

        String coreServer = MCRConfiguration2.getString(coreServerKey)
            .orElse(MCRSolrConstants.DEFAULT_SOLR_SERVER_URL);

        String configSetTemplate = MCRConfiguration2.getString(configSetTemplateKey).orElse(null);

        Integer shardCount = MCRConfiguration2.getInt(shardCountKey).orElse(MCRSolrCore.DEFAULT_SHARD_COUNT);

        String coreTypeKey = MCRSolrConstants.SOLR_CORE_PREFIX + coreID + MCRSolrConstants.SOLR_CORE_TYPE_SUFFIX;
        Set<MCRSolrCoreType> coreTypes = MCRConfiguration2.getOrThrow(coreTypeKey, MCRConfiguration2::splitValue)
            .map(MCRSolrCoreType::new)
            .collect(Collectors.toSet());

        return new MCRSolrCore(coreServer, coreName, configSetTemplate, shardCount, coreTypes);
    }

    public static MCRSolrCore addCore(String coreID, MCRSolrCore core) {
        coreMap.put(coreID, core);
        return core;
    }

    /**
     * Add a SOLR core instance to the list
     *
     * @param core the MCRSolrCore instance 
     */
    public static void add(String coreID, MCRSolrCore core) {
        coreMap.put(coreID, core);
    }

    /**
     * Remove a SOLR core instance from the list
     *
     * @param coreID the name of the MCRSolrCore instance
     */
    public static Optional<MCRSolrCore> remove(String coreID) {
        return Optional.ofNullable(coreMap.remove(coreID));
    }

    /**
     * @param coreID the id of the core
     * @return a core with a specific id
     */
    public static Optional<MCRSolrCore> get(String coreID) {
        return Optional.ofNullable(coreMap.get(coreID));
    }

    public static MCRSolrCore getMainSolrCore() {
        return get(MCRSolrConstants.MAIN_CORE_TYPE)
            .orElseThrow(() -> new MCRConfigurationException("The core main is not configured!"));
    }

    /**
     * Returns the solr cores which should be used for a specific type.
     * @param type the type of the core
     * @return a list of cores
     */
    public static List<MCRSolrCore> getCoresForType(MCRSolrCoreType type) {
        return coreMap.values().stream().filter(c -> c.getTypes().contains(type)).collect(Collectors.toList());
    }

    /**
     * Returns the solr client of the default core.
     */
    public static SolrClient getMainSolrClient() {
        return getMainSolrCore().getClient();
    }

    /**
     * Returns the concurrent solr client of the default core.
     */
    public static SolrClient getMainConcurrentSolrClient() {
        return getMainSolrCore().getConcurrentClient();
    }

    /**
     * @return the read only core map wich contains the coreId and the core
     */
    public static Map<String, MCRSolrCore> getCoreMap() {
        return Collections.unmodifiableMap(coreMap);
    }

}
