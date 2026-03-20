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

package org.mycore.solr.schema;

import static java.util.Map.entry;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationInputStream;
import org.mycore.solr.MCRSolrIndex;
import org.mycore.solr.MCRSolrIndexRegistryManager;
import org.mycore.solr.MCRSolrUtils;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;
import org.mycore.solr.search.MCRSolrSearchUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * This class provides methods to reload a SOLR configuration using the SOLR configuration API
 * see https://lucene.apache.org/solr/guide/8_6/config-api.html
 *
 * @author Robert Stephan
 * @author Jens Kupferschmidt
 */
public class MCRSolrConfigReloader {

    /**
    from https://lucene.apache.org/solr/guide/8_6/config-api.html
    key = lowercase object name from config api command (add-*, update-* delete-*)
    value = keyName in SOLR config json (retrieved via URL ../core-name/config)
    */
    private static final Map<String, String> SOLR_CONFIG_OBJECT_NAMES = Map.ofEntries(
        entry("requesthandler", "requestHandler"), //checked -> id in subfield "name"
        entry("searchcomponent", "searchComponent"), //checked  -> id in subfield "name"
        entry("initparams", "initParams"), //checked - id in key (TODO special handling)
        entry("queryresponsewriter", "queryResponseWriter"), //checked  -> id in subfield "name"
        entry("queryparser", "queryParser"),
        entry("valuesourceparser", "valueSourceParser"),
        entry("transformer", "transformer"),
        entry("updateprocessor", "updateProcessor"), //checked  -> id in subfield "name"
        entry("queryconverter", "queryConverter"),
        entry("listener", "listener"), //checked -> id in subfield "event" -> special handling
        entry("runtimelib", "runtimeLib"));

    private static final List<String> SOLR_CONFIG_PROPERTY_COMMANDS = List.of("set-property", "unset-property");

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String SOLR_CONFIG_UPDATE_FILE_NAME = "solr-config.json";

    /**
     * Removed items from SOLR configuration overlay. This removal works over all in the property
     * MCR.Solr.ObserverConfigTypes defined SOLR configuration parts. For each entry the
     * method will process a SOLR delete command via API.
     *
     * @param configType the name of the configuration directory containing the SOLR core configuration
     * @param coreID the ID of the core, which the configuration should be applied to
     */
    public static void reset(String configType, String coreID) {
        LOGGER.info(() -> "Resetting config definitions for core " + coreID + " using configuration " + configType);

        MCRSolrIndex index = MCRSolrIndexRegistryManager.obtainRegistry()
            .getIndex(coreID)
            .orElseThrow(() -> MCRSolrUtils.getIndexConfigMissingException(coreID));

        JsonObject currentSolrConfig = retrieveCurrentSolrConfigOverlay(index);
        JsonObject configPart = currentSolrConfig.getAsJsonObject("overlay");

        for (String observedType : getObserverConfigTypes()) {
            JsonObject overlaySection = configPart.getAsJsonObject(observedType);
            if (overlaySection == null) {
                continue;
            }
            Set<Map.Entry<String, JsonElement>> configuredComponents = overlaySection.entrySet();
            final String deleteSectionCommand = "delete-" + observedType.toLowerCase(Locale.ROOT);
            if (configuredComponents.isEmpty() || !isKnownSolrConfigCommand(deleteSectionCommand)) {
                continue;
            }
            for (Map.Entry<String, JsonElement> configuredComponent : configuredComponents) {
                final JsonObject deleteCommand = new JsonObject();
                deleteCommand.addProperty(deleteSectionCommand, configuredComponent.getKey());
                LOGGER.debug(deleteCommand);
                executeSolrCommand(index, deleteCommand);
            }
        }
    }

    /**
     * This method modified the SOLR configuration definition based on all solr/{coreType}/solr-config.json 
     * in the MyCoRe-Maven modules resource path.
     *
     * @param configType the name of the configuration directory containing the SOLR core configuration
     * @param coreID the ID of the core, which the configuration should be applied to
     */
    public static void processConfigFiles(String configType, String coreID) {
        LOGGER.info(() -> "Load config definitions for core " + coreID + " using configuration " + configType);
        try {
            MCRSolrIndex index = MCRSolrIndexRegistryManager.obtainRegistry().requireIndex(coreID);

            List<String> observedTypes = getObserverConfigTypes();
            JsonObject currentSolrConfig = retrieveCurrentSolrConfig(index);

            Map<String, byte[]> configFileContents = MCRConfigurationInputStream.getConfigFileContents(
                "solr/" + configType + "/" + SOLR_CONFIG_UPDATE_FILE_NAME);
            for (byte[] configFileData : configFileContents.values()) {
                String content = new String(configFileData, StandardCharsets.UTF_8);
                JsonElement json = JsonParser.parseString(content);
                if (!json.isJsonArray()) {
                    JsonElement e = json;
                    json = new JsonArray();
                    json.getAsJsonArray().add(e);
                }

                for (JsonElement command : json.getAsJsonArray()) {
                    LOGGER.debug(command);
                    processConfigCommand(index, command, currentSolrConfig, observedTypes);
                }
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    /**
     * get the content of property MCR.Solr.ObserverConfigTypes as List
     * @return the list of observed SOLR configuration types, a.k.a. top-level sections of config API
     */
    private static List<String> getObserverConfigTypes() {
        return MCRConfiguration2
            .getString("MCR.Solr.ObserverConfigTypes")
            .map(MCRConfiguration2::splitValue)
            .orElseGet(Stream::empty)
            .collect(Collectors.toList());
    }

    /**
     * processes a single SOLR configuration command
     * @param coreURL - the URL of the core
     * @param command - the command in JSON syntax
     */
    private static void processConfigCommand(MCRSolrIndex coreURL, JsonElement command, JsonObject currentSolrConfig,
        List<String> observedTypes) {
        if (command.isJsonObject()) {
            //get first and only? property of the command object
            final JsonObject commandJsonObject = command.getAsJsonObject();
            Entry<String, JsonElement> commandObject = commandJsonObject.entrySet().iterator().next();
            final String configCommand = commandObject.getKey();
            final String configType = StringUtils.substringAfter(configCommand, "add-");

            if (isKnownSolrConfigCommand(configCommand)) {

                if (observedTypes.contains(configType) && configCommand.startsWith("add-") &&
                    commandObject.getValue() instanceof JsonObject) {
                    final JsonElement configCommandName = commandObject.getValue().getAsJsonObject().get("name");
                    if (isConfigTypeAlreadyAdded(configType, configCommandName, currentSolrConfig)) {
                        LOGGER.info(() -> "Current configuration has already an " + configCommand
                            + " with name " + configCommandName.getAsString()
                            + ". Rewrite config command as update-" + configType);
                        commandJsonObject.add("update-" + configType, commandJsonObject.get(configCommand));
                        commandJsonObject.remove(configCommand);
                    }
                }
                executeSolrCommand(coreURL, commandJsonObject);
            }
        }

    }

    /**
     * Sends a command to SOLR server
     * @param index to which the command will be send
     * @param command the command
     */
    private static void executeSolrCommand(MCRSolrIndex index, JsonObject command) {
        GenericSolrRequest request = new GenericSolrRequest(CollectionAdminRequest.METHOD.POST,
            "/config");
        request.setRequiresCollection(true);
        request.withContent(command.toString().getBytes(StandardCharsets.UTF_8), "application/json");

        MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(request,
            MCRSolrAuthenticationLevel.ADMIN);
        String commandPrefix = command.keySet().stream().findFirst().orElse("unknown command");

        try (InputStream is = MCRSolrSearchUtils.streamRequest(index.getClient(), request, "json")) {
            String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            LOGGER.debug(() -> "SOLR config " + commandPrefix + " command was successful \n" + response);
        } catch (SolrServerException e) {
            LOGGER
                .error(() -> "SOLR config " + commandPrefix + " error: " + e.getMessage() + "\n" + command, e);
        } catch (IOException e) {
            LOGGER.error(() -> "Could not execute the following Solr config command:\n" + command, e);
        }
    }

    /**
     * Checks if a given configType with passed name already was added to current SORL configuration
     * @param configType - Type of localized SOLR component e.g. request handlers, search components
     * @param name - identification of configuration type
     * @param solrConfig - current SOLR configuration
     * @return - Is there already an entry in current SOLR configuration
     */
    private static boolean isConfigTypeAlreadyAdded(String configType, JsonElement name, JsonObject solrConfig) {

        JsonObject configPart = solrConfig.getAsJsonObject("config");
        JsonObject observedConfig = configPart.getAsJsonObject(configType);

        return observedConfig.has(name.getAsString());
    }

    /**
     * retrieves the current SOLR configuration for the given core 
     * @param solrIndex from which the current SOLR configuration will be load
     * @return the configuration as JSON object
     */
    private static JsonObject retrieveCurrentSolrConfig(MCRSolrIndex solrIndex) {
        GenericSolrRequest request = new GenericSolrRequest(CollectionAdminRequest.METHOD.GET,
            "/config");
        request.setRequiresCollection(true);
        MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(request,
            MCRSolrAuthenticationLevel.ADMIN);
        return getJSON(solrIndex, request);
    }

    /**
     * retrieves the current SOLR configuration overlay for the given core
     * @param solrIndex from which the current SOLR configuration will be load
     * @return the configuration as JSON object
     */
    private static JsonObject retrieveCurrentSolrConfigOverlay(MCRSolrIndex solrIndex) {
        GenericSolrRequest request = new GenericSolrRequest(CollectionAdminRequest.METHOD.GET,
            "/config/overlay");
        request.setRequiresCollection(true);
        MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(request,
            MCRSolrAuthenticationLevel.ADMIN);
        return getJSON(solrIndex, request);
    }

    private static JsonObject getJSON(MCRSolrIndex solrIndex, GenericSolrRequest request) {
        JsonObject convertedObject = null;
        try (InputStream jsonIS = MCRSolrSearchUtils.streamRequest(solrIndex.getClient(), request,
            "json"); InputStreamReader reader = new InputStreamReader(jsonIS, StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, JsonObject.class);
        } catch (IOException | SolrServerException e) {
            LOGGER.error("Could not read current Solr configuration", e);
        } catch (JsonSyntaxException e) {
            LOGGER.error("Current json configuration is not a valid json", e);
        }
        return convertedObject;
    }

    /**
     *
     * @param cmd the SOLR API command
     * @return true, if the command is in the list of known SOLR commands.
     */

    private static boolean isKnownSolrConfigCommand(String cmd) {
        String cfgObjName = cmd.substring(cmd.indexOf('-') + 1).toLowerCase(Locale.ROOT);
        return ((cmd.startsWith("add-") || cmd.startsWith("update-") || cmd.startsWith("delete-"))
            && (SOLR_CONFIG_OBJECT_NAMES.containsKey(cfgObjName)))
            || SOLR_CONFIG_PROPERTY_COMMANDS.contains(cmd);
    }

}
