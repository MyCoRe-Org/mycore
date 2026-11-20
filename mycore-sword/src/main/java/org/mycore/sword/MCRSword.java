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

package org.mycore.sword;

import static org.mycore.common.config.instantiator.MCRInstanceConfiguration.CLASS_SUFFIX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.sword.application.MCRSwordCollectionProvider;
import org.mycore.sword.application.MCRSwordLifecycleConfiguration;

/**
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRSword {

    private static final Logger LOGGER = LogManager.getLogger();

    private static volatile Map<String, MCRSwordCollectionProvider> collections;

    private static volatile Map<String, List<String>> workspaceCollectionTable;

    private static void initConfigThreadSafe() {
        if (collections == null) {
            synchronized (MCRSword.class) {
                if (collections == null) {
                    initConfig();
                }
            }
        }
    }

    private static void initConfig() {
        LOGGER.info("--- INITIALIZE SWORD SERVER ---");
        Map<String, MCRSwordCollectionProvider> newCollections = new ConcurrentHashMap<>();
        Map<String, List<String>> newWorkspaceCollectionTable = new ConcurrentHashMap<>();
        parseCollectionNames(MCRConfiguration2.getPropertiesMap())
                .stream()
                .map(name -> name.split(Pattern.quote("."))) // split to workspace name and collection name
                .filter(array -> array.length == 2) // exactly one workspace and one collection name
                .forEach(wsCol -> initWorkspaceCollection(wsCol[0], wsCol[1],
                        newCollections, newWorkspaceCollectionTable));
        workspaceCollectionTable = newWorkspaceCollectionTable;
        collections = newCollections;
        addCollectionShutdownHook();
    }

    private static void initWorkspaceCollection(String workspace, String collection,
                                                Map<String, MCRSwordCollectionProvider> targetCollections,
                                                Map<String, List<String>> targetWorkspaceCollectionTable) {
        LOGGER.info("Found collection: {} in workspace {}", collection, workspace);
        String name = MCRSwordConstants.MCR_SWORD_COLLECTION_PREFIX + workspace + "." + collection;

        LOGGER.info("Try to init : {}", name);
        MCRSwordCollectionProvider collectionProvider = MCRConfiguration2
            .getInstanceOfOrThrow(MCRSwordCollectionProvider.class, name);
        targetCollections.put(collection, collectionProvider);
        final MCRSwordLifecycleConfiguration lifecycleConfiguration = new MCRSwordLifecycleConfiguration(collection);
        collectionProvider.init(lifecycleConfiguration);

        List<String> collectionsOfWorkspace;
        if (targetWorkspaceCollectionTable.containsKey(workspace)) {
            collectionsOfWorkspace = targetWorkspaceCollectionTable.get(workspace);
        } else {
            collectionsOfWorkspace = new ArrayList<>();
            targetWorkspaceCollectionTable.put(workspace, collectionsOfWorkspace);
        }
        collectionsOfWorkspace.add(collection);
    }

    private static void addCollectionShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown Sword Collections");
            collections.values().forEach(MCRSwordCollectionProvider::destroy);
        }));
    }

    public static MCRSwordCollectionProvider getCollection(String collectionName) {
        initConfigThreadSafe();
        return collections.get(collectionName);
    }

    static Set<String> parseCollectionNames(Map<String, String> properties) {
        Set<String> keys = properties.keySet().stream()
                .filter(key -> key.startsWith(MCRSwordConstants.MCR_SWORD_COLLECTION_PREFIX))
                .map(key -> key.substring(MCRSwordConstants.MCR_SWORD_COLLECTION_PREFIX.length()))
                .collect(Collectors.toSet());
        Set<String> collectionNames = keys.stream()
                .filter(key -> key.endsWith(CLASS_SUFFIX))
                .map(key -> key.substring(0, key.length() - CLASS_SUFFIX.length()))
                .collect(Collectors.toSet());
        List<String> legacyKeys = keys.stream()
                .filter(key -> !key.endsWith(CLASS_SUFFIX))
                .filter(key -> collectionNames.stream()
                        .noneMatch(name -> key.equals(name) || key.startsWith(name + ".")))
                .map(key -> MCRSwordConstants.MCR_SWORD_COLLECTION_PREFIX + key)
                .sorted()
                .toList();
        if (!legacyKeys.isEmpty()) {
            throw new MCRConfigurationException(legacyKeys.stream()
                    .map(key -> key + " ==> " + key + CLASS_SUFFIX)
                    .collect(Collectors.joining("\n",
                            "SWORD collections must be configured with " + CLASS_SUFFIX + " suffix:\n", "")));
        }
        return collectionNames;
    }

    public static List<String> getCollectionNames() {
        initConfigThreadSafe();
        return new ArrayList<>(collections.keySet());
    }

    public static List<String> getWorkspaces() {
        initConfigThreadSafe();
        return workspaceCollectionTable.keySet().stream().toList();
    }

    public static List<String> getCollectionsOfWorkspace(String workspace) {
        initConfigThreadSafe();
        return Collections.unmodifiableList(workspaceCollectionTable.get(workspace));
    }

}
