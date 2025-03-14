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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.MCRConfigurationInputStream;
import org.mycore.services.http.MCRHttpUtils;
import org.mycore.solr.MCRSolrCore;
import org.mycore.solr.MCRSolrCoreManager;
import org.mycore.solr.MCRSolrUtils;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * This class provides methods to reload a SOLR schema using the
 * <a href="https://lucene.apache.org/solr/guide/7_3/schema-api.html">SOLR schema API</a>
 *
 * @author Robert Stephan
 * @author Jens Kupferschmidt
 */
public class MCRSolrSchemaReloader {
    private static final MCRSolrAuthenticationManager SOLR_AUTHENTICATION_MANAGER =
        MCRSolrAuthenticationManager.obtainInstance();

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String SOLR_SCHEMA_UPDATE_FILE_NAME = "solr-schema.json";

    private static final List<String> SOLR_DEFAULT_FIELDS = Arrays.asList("id", "_version_", "_root_", "_text_");

    private static final List<String> SOLR_DEFAULT_FIELD_TYPES = Arrays.asList("plong", "string", "text_general");

    /**
     * Removes all fields, dynamicFields, copyFields and fieldTypes in the SOLR schema for the given core. The fields,
     * dynamicFields, and types in the lists SOLR_DEFAULT_FIELDS, SOLR_DEFAULT_DYNAMIC_FIELDS,
     * SOLR_DEFAULT_DYNAMIC_FIELDS are excluded from remove.
     *
     * @param configType the name of the configuration directory containing the Solr core configuration
     * @param coreID the ID of the core, which the configuration should be applied to
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public static void reset(String configType, String coreID) {
        LOGGER.info(() -> "Resetting SOLR schema for core " + coreID + " using configuration " + configType);
        try {
            SolrClient solrClient = MCRSolrCoreManager.get(coreID).map(MCRSolrCore::getClient)
                .orElseThrow(() -> new MCRConfigurationException("The core " + coreID + " is not configured!"));

            deleteCopyFields(solrClient);
            LOGGER.debug(() -> "CopyFields cleaned for core " + coreID + " for configuration " + configType);

            deleteFields(solrClient);
            LOGGER.debug(() -> "Fields cleaned for core " + coreID + " for configuration " + configType);

            deleteDynamicFields(solrClient);
            LOGGER.debug(() -> "DynamicFields cleaned for core " + coreID + " for configuration " + configType);

            deleteFieldTypes(solrClient);
            LOGGER.debug(() -> "FieldTypes cleaned for core " + coreID + " for configuration " + configType);

        } catch (IOException | SolrServerException e) {
            LOGGER.error(e);
        }
    }

    private static void deleteFieldTypes(SolrClient solrClient)
        throws SolrServerException, IOException {
        SchemaRequest.FieldTypes fieldTypesReq = new SchemaRequest.FieldTypes();
        SOLR_AUTHENTICATION_MANAGER.applyAuthentication(fieldTypesReq,
            MCRSolrAuthenticationLevel.ADMIN);
        for (FieldTypeRepresentation fieldType : fieldTypesReq.process(solrClient).getFieldTypes()) {
            String fieldTypeName = fieldType.getAttributes().get("name").toString();
            if (!SOLR_DEFAULT_FIELD_TYPES.contains(fieldTypeName)) {
                LOGGER.debug(() -> "remove SOLR FieldType " + fieldTypeName);
                SchemaRequest.DeleteFieldType delField = new SchemaRequest.DeleteFieldType(fieldTypeName);
                SOLR_AUTHENTICATION_MANAGER.applyAuthentication(delField,
                    MCRSolrAuthenticationLevel.ADMIN);
                delField.process(solrClient);
            }
        }
    }

    private static void deleteDynamicFields(SolrClient solrClient)
        throws SolrServerException, IOException {
        SchemaRequest.DynamicFields dynFieldsReq = new SchemaRequest.DynamicFields();
        SOLR_AUTHENTICATION_MANAGER.applyAuthentication(dynFieldsReq,
            MCRSolrAuthenticationLevel.ADMIN);
        for (Map<String, Object> field : dynFieldsReq.process(solrClient).getDynamicFields()) {
            String fieldName = field.get("name").toString();
            LOGGER.debug(() -> "remove SOLR DynamicField " + fieldName);
            SchemaRequest.DeleteDynamicField delField = new SchemaRequest.DeleteDynamicField(fieldName);
            SOLR_AUTHENTICATION_MANAGER.applyAuthentication(delField,
                MCRSolrAuthenticationLevel.ADMIN);
            delField.process(solrClient);

        }
    }

    private static void deleteFields(SolrClient solrClient)
        throws SolrServerException, IOException {
        SchemaRequest.Fields fieldsReq = new SchemaRequest.Fields();
        SOLR_AUTHENTICATION_MANAGER.applyAuthentication(fieldsReq, MCRSolrAuthenticationLevel.ADMIN);
        for (Map<String, Object> field : fieldsReq.process(solrClient).getFields()) {
            String fieldName = field.get("name").toString();
            if (!SOLR_DEFAULT_FIELDS.contains(fieldName)) {
                LOGGER.debug(() -> "remove SOLR Field " + fieldName);
                SchemaRequest.DeleteField delField = new SchemaRequest.DeleteField(fieldName);
                SOLR_AUTHENTICATION_MANAGER.applyAuthentication(delField,
                    MCRSolrAuthenticationLevel.ADMIN);
                delField.process(solrClient);
            }
        }
    }

    private static void deleteCopyFields(SolrClient solrClient)
        throws SolrServerException, IOException {
        SchemaRequest.CopyFields copyFieldsReq = new SchemaRequest.CopyFields();
        SOLR_AUTHENTICATION_MANAGER.applyAuthentication(copyFieldsReq,
            MCRSolrAuthenticationLevel.ADMIN);
        for (Map<String, Object> copyField : copyFieldsReq.process(solrClient).getCopyFields()) {
            String fieldSrc = copyField.get("source").toString();
            List<String> fieldDest = new ArrayList<>();
            fieldDest.add(copyField.get("dest").toString());
            LOGGER.debug(() -> "remove SOLR CopyField " + fieldSrc + " --> " + fieldDest.getFirst());
            SchemaRequest.DeleteCopyField delCopyField = new SchemaRequest.DeleteCopyField(fieldSrc, fieldDest);
            SOLR_AUTHENTICATION_MANAGER.applyAuthentication(delCopyField,
                MCRSolrAuthenticationLevel.ADMIN);
            delCopyField.process(solrClient);
        }
    }

    /**
     * This method modified the SOLR schema definition based on all solr/{coreType}/solr-schema.json
     * in the MyCoRe-Maven modules resource path.
     *
     * @param configType the name of the configuration directory containg the Solr core configuration
     * @param coreID the ID of the core, which the configuration should be applied to
     */
    public static void processSchemaFiles(String configType, String coreID) {
        MCRSolrCore solrCore = MCRSolrCoreManager.get(coreID)
            .orElseThrow(() -> MCRSolrUtils.getCoreConfigMissingException(coreID));

        LOGGER.info(() -> "Load schema definitions for core " + coreID + " using configuration " + configType);
        try (HttpClient httpClient = MCRHttpUtils.getHttpClient()) {
            Collection<byte[]> schemaFileContents = MCRConfigurationInputStream.getConfigFileContents(
                "solr/" + configType + "/" + SOLR_SCHEMA_UPDATE_FILE_NAME).values();
            for (byte[] schemaFileData : schemaFileContents) {
                InputStreamReader schemaReader = new InputStreamReader(new ByteArrayInputStream(schemaFileData),
                    StandardCharsets.UTF_8);
                JsonElement json = JsonParser.parseReader(schemaReader);
                if (!json.isJsonArray()) {
                    JsonElement e = json;
                    json = new JsonArray();
                    json.getAsJsonArray().add(e);
                }
                for (JsonElement e : json.getAsJsonArray()) {
                    LOGGER.debug(e);
                    String command = e.toString();

                    HttpRequest.Builder solrRequestBuilder = MCRSolrUtils.getRequestBuilder()
                        .uri(URI.create(solrCore.getV1CoreURL() + "/schema"))
                        .header("Content-type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(command));
                    SOLR_AUTHENTICATION_MANAGER.applyAuthentication(solrRequestBuilder,
                        MCRSolrAuthenticationLevel.ADMIN);
                    String commandPrefix = command.indexOf('-') != -1 ? command.substring(2, command.indexOf('-'))
                        : "unknown command";

                    HttpResponse<String> response = httpClient.send(solrRequestBuilder.build(),
                        HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        LOGGER.debug("SOLR schema {} successful \n{}", () -> commandPrefix, response::body);
                    } else {

                        LOGGER
                            .error("SOLR schema {} error: {} {}\n{}", () -> commandPrefix, response::statusCode,
                                () -> MCRHttpUtils.getReasonPhrase(response.statusCode()), response::body);
                    }
                }
            }
        } catch (InterruptedException | IOException e) {
            LOGGER.error(e);
        }
    }

}
