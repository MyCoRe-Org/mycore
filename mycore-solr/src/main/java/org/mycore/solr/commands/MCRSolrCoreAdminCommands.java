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

package org.mycore.solr.commands;

import static org.mycore.solr.MCRSolrConstants.DEFAULT_SOLR_SERVER_URL;
import static org.mycore.solr.MCRSolrConstants.SOLR_CONFIG_PREFIX;
import static org.mycore.solr.MCRSolrConstants.SOLR_CORE_CONFIGSET_TEMPLATE_SUFFIX;
import static org.mycore.solr.MCRSolrConstants.SOLR_CORE_NAME_SUFFIX;
import static org.mycore.solr.MCRSolrConstants.SOLR_CORE_PREFIX;
import static org.mycore.solr.MCRSolrConstants.SOLR_CORE_SERVER_SUFFIX;
import static org.mycore.solr.MCRSolrConstants.SOLR_CORE_SHARD_COUNT_SUFFIX;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.solr.MCRSolrCore;
import org.mycore.solr.MCRSolrCoreManager;
import org.mycore.solr.MCRSolrCoreType;
import org.mycore.solr.schema.MCRSolrConfigReloader;
import org.mycore.solr.schema.MCRSolrSchemaReloader;

@MCRCommandGroup(
        name = "SOLR Core Admin Commands")
public class MCRSolrCoreAdminCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * This command displays the MyCoRe properties,
     * which have to be set, to reload the current Solr / Solr Core configuration
     * on the next start of the MyCoRe application.
     */
    @MCRCommand(
        syntax = "show solr configuration",
        help = "displays MyCoRe properties for the current Solr configuration",
        order = 10)
    public static void listConfig() {
        LOGGER.info("List core configuration: {}{}{}{}", System.lineSeparator(),
            SOLR_CONFIG_PREFIX + "ServerURL=" + DEFAULT_SOLR_SERVER_URL,
            System.lineSeparator(),
            MCRSolrCoreManager.getCoreMap().entrySet().stream().map(
                (entry) -> {
                    String coreID = entry.getKey();
                    MCRSolrCore core = entry.getValue();

                    String format = "{0}{1}{2}={3}";
                    if (!DEFAULT_SOLR_SERVER_URL.equals(core.getServerURL())) {
                        format += "\n{0}{1}{5}={4}";
                    }
                    if (core.getConfigSet() != null) {
                        format += "\n{0}{1}{6}={7}";
                    }
                    if (core.getShardCount() > 1) {
                        format += "\n{0}{1}{8}={9}";
                    }

                    return new MessageFormat(format, Locale.ROOT).format(new String[] { SOLR_CORE_PREFIX, coreID,
                        SOLR_CORE_NAME_SUFFIX, core.getName(), core.getServerURL(), SOLR_CORE_SERVER_SUFFIX,
                        SOLR_CORE_CONFIGSET_TEMPLATE_SUFFIX, core.getConfigSet(),
                        SOLR_CORE_SHARD_COUNT_SUFFIX, core.getShardCount() + "" });
                }).collect(Collectors.joining("\n")));
    }

    @MCRCommand(
        syntax = "register solr core with name {0} and configset {1} and shards {2} on server {3} as core {4}" +
            " for types {5}",
        help = "registers a Solr core within MyCoRe",
        order = 30)
    public static void registerSolrCore(String remoteCoreName, String configSetName, int shards, String server,
        String coreID, String types) {
        Set<MCRSolrCoreType> coreTypes = Stream.of(types.split("[, ]"))
            .map(MCRSolrCoreType::new)
            .collect(Collectors.toSet());

        MCRSolrCore core = new MCRSolrCore(server, remoteCoreName, configSetName, shards, coreTypes);
        MCRSolrCoreManager.addCore(coreID, core);
    }

    @MCRCommand(
        syntax = "register solr core with name {0} and configset {1} on server {2} as core {3} for types {4}",
        help = "registers a Solr core within MyCoRe",
        order = 35)
    public static void registerSolrCore(String remoteCoreName, String configSetName, String server, String coreID,
        String types) {
        Set<MCRSolrCoreType> coreTypes = Stream.of(types.split("[, ]"))
            .map(MCRSolrCoreType::new)
            .collect(Collectors.toSet());

        MCRSolrCore core = new MCRSolrCore(server, remoteCoreName, configSetName, 1, coreTypes);
        MCRSolrCoreManager.addCore(coreID, core);
    }


    @MCRCommand(
            syntax = "register solr core with name {0} and configset {1} as core {2} for types {3}",
            help = "registers a Solr core within MyCoRe",
            order = 37)
    public static void registerSolrCore(String remoteCoreName, String configSetName, String coreID,
                                        String types) {
        registerSolrCore(remoteCoreName, configSetName, DEFAULT_SOLR_SERVER_URL, coreID, types);
    }


    @MCRCommand(
        syntax = "register solr core with name {0} on server {1} as core {2}",
        help = "registers a Solr core within MyCoRe",
        order = 40)
    public static void registerSolrCore(String remoteCoreName, String server, String coreID) {
        MCRSolrCore core = new MCRSolrCore(server, remoteCoreName, null, 1, Collections.emptySet());
        MCRSolrCoreManager.addCore(coreID, core);
    }

    @MCRCommand(
        syntax = "register solr core with name {0} as core {1}",
        help = "registers a Solr core on the configured default Solr server within MyCoRe",
        order = 50)
    public static void registerSolrCore(String remoteCoreName, String coreID) {
        registerSolrCore(remoteCoreName, DEFAULT_SOLR_SERVER_URL, coreID);
    }

    @MCRCommand(
        syntax = "switch solr core {0} with core {1}",
        help = "switches between two Solr core configurations",
        order = 60)
    public static void switchSolrCore(String coreID1, String coreID2) {

        MCRSolrCore core1 = MCRSolrCoreManager.get(coreID1).orElseThrow();
        MCRSolrCore core2 = MCRSolrCoreManager.get(coreID2).orElseThrow();

        MCRSolrCoreManager.add(coreID1, core2);
        MCRSolrCoreManager.add(coreID2, core1);
    }

    /**
     * This command recreates the managed-schema.xml and solrconfig.xml files. First it removes all
     * schema definitions, except for some MyCoRe default types and fields. Second it parses the available
     * MyCoRe modules and components and adds / updates / deletes the schema definition.
     * Finally it does the same for the solrconfig.xml definition.
     *
     * see https://github.com/MyCoRe-Org/mycore_solr_configset_main
     *
     * @param coreID the core type of the core that should be reloaded; the MyCoRe default application
     * coreID is <b>main</b>
     */
    @MCRCommand(
        syntax = "reload solr configuration {0} in core {1}",
        help = "reloads the schema and the configuration in Solr "
            + "by using the Solr schema api for core with the id {0}",
        order = 70)
    public static void reloadSolrConfiguration(String configType, String coreID) {
        MCRSolrConfigReloader.reset(configType, coreID);
        MCRSolrSchemaReloader.reset(configType, coreID);
        MCRSolrSchemaReloader.processSchemaFiles(configType, coreID);
        MCRSolrConfigReloader.processConfigFiles(configType, coreID);
    }
}
