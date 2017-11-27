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

package org.mycore.sword;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.sword.application.MCRSwordCollectionProvider;
import org.mycore.sword.application.MCRSwordLifecycleConfiguration;

/**
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRSword {

    private static Logger LOGGER = LogManager.getLogger(MCRSword.class);

    private static Hashtable<String, MCRSwordCollectionProvider> collections = null;

    private static Hashtable<String, List<String>> workspaceCollectionTable = null;

    private static void initConfig() {
        if (collections == null) {
            collections = new Hashtable<>();
            workspaceCollectionTable = new Hashtable<>();
            final MCRConfiguration mcrConfiguration = MCRConfiguration.instance();
            Map<String, String> propertiesMap = mcrConfiguration.getPropertiesMap();
            final int lenghtOfPropertyPrefix = MCRSwordConstants.MCR_SWORD_COLLECTION_PREFIX.length();
            LOGGER.info("--- INITIALIZE SWORD SERVER ---");

            propertiesMap.keySet().stream()
                .filter(prop -> prop.startsWith(MCRSwordConstants.MCR_SWORD_COLLECTION_PREFIX)) // remove all which are not collections
                .filter(prop -> !prop.trim().equals(MCRSwordConstants.MCR_SWORD_COLLECTION_PREFIX)) // remove all which have no suffix
                .map(prop -> prop.substring(lenghtOfPropertyPrefix)) // remove MCR_SWORD_COLLECTION_PREFIX
                .map(prop -> prop.split(Pattern.quote("."), 2)) // split to workspace name and collection name
                .filter(prop -> prop.length == 2) // remove all whith no workspace or collection name
                .forEach(workspaceCollectionEntry -> {
                    final String collection = workspaceCollectionEntry[1];
                    final String workspace = workspaceCollectionEntry[0];

                    LOGGER.info("Found collection: {} in workspace {}", collection, workspace);
                    String name = MCRSwordConstants.MCR_SWORD_COLLECTION_PREFIX + workspace + "." + collection;

                    LOGGER.info("Try to init : {}", name);
                    MCRSwordCollectionProvider collectionProvider = mcrConfiguration.getInstanceOf(name);
                    collections.put(collection, collectionProvider);
                    final MCRSwordLifecycleConfiguration lifecycleConfiguration = new MCRSwordLifecycleConfiguration(
                        collection);
                    collectionProvider.init(lifecycleConfiguration);

                    // This Map is needed to speed up the build of the {@link org.mycore.mir.sword2.manager.MCRServiceDocumentManager}
                    List<String> collectionsOfWorkspace;
                    if (workspaceCollectionTable.containsKey(workspace)) {
                        collectionsOfWorkspace = workspaceCollectionTable.get(workspace);
                    } else {
                        collectionsOfWorkspace = new ArrayList<>();
                        workspaceCollectionTable.put(workspace, collectionsOfWorkspace);
                    }
                    collectionsOfWorkspace.add(collection);
                });

            addCollectionShutdownHook();
        }

    }

    private static void addCollectionShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown Sword Collections");
            collections.values().forEach(MCRSwordCollectionProvider::destroy);
        }));
    }

    public static MCRSwordCollectionProvider getCollection(String collectionName) {
        initConfig();
        return collections.get(collectionName);
    }

    public static List<String> getCollectionNames() {
        initConfig();
        return new ArrayList<>(collections.keySet());
    }

    public static List<String> getWorkspaces() {
        initConfig();
        return Collections.unmodifiableList(workspaceCollectionTable.keySet().stream().collect(Collectors.toList()));
    }

    public static List<String> getCollectionsOfWorkspace(String workspace) {
        initConfig();
        return Collections.unmodifiableList(workspaceCollectionTable.get(workspace));
    }

}
