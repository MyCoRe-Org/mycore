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

package org.mycore.solr.schema;

import static org.mycore.solr.MCRSolrConstants.SOLR_SERVER_URL;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;
import org.mycore.common.config.MCRConfigurationInputStream;
import org.mycore.solr.MCRSolrClientFactory;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * This class provides methods to reload a SOLR schema using the SOLR schema API
 * @see https://lucene.apache.org/solr/guide/7_3/schema-api.html
 *
 * @author Robert Stephan
 * @author jens Kupferschmidt
 */
public class MCRSolrSchemaReloader {
    private static Logger LOGGER = LogManager.getLogger(MCRSolrSchemaReloader.class);

    private static String SOLR_SCHEMA_UPDATE_FILE_NAME = "solr-schema.json";

    private static List<String> SOLR_DEFAULT_FIELDS = Arrays.asList("id", "_version_", "_root_", "_text_");

    private static List<String> SOLR_DEFAULT_DYNAMIC_FIELDS = Arrays.asList("*", "ignored_*");

    private static List<String> SOLR_DEFAULT_FIELDTYPES = Arrays.asList("ignored", "string", "strings", "boolean",
        "booleans", "pint", "plong", "pfloat", "pdouble", "pints", "plongs", "pfloats", "pdoubles", "pdate", "pdates",
        "string", "binary", "text_general");

    /**
     * Remove all fields, dynamicFields, copyFields and fieldTypes in the SOLR schema for the given core. The fields,
     * dynamicFields, and types in the lists SOLR_DEFAULT_FIELDS, SOLR_DEFAULT_DYNAMIC_FIELDS, SOLR_DEFAULT_DYNAMIC_FIELDS
     * are excluded from remove.
     * 
     * @param coreName the name of the core
     */
    public static void clearSchema(String coreName) {

        LOGGER.info("Clear SOLR schema in core " + coreName);
        try {
            SolrClient solrClient = MCRSolrClientFactory.get(coreName).getClient();

            SchemaRequest.CopyFields copyFieldsReq = new SchemaRequest.CopyFields();
            for (Map<String, Object> copyField : copyFieldsReq.process(solrClient).getCopyFields()) {
                String fieldSrc = copyField.get("source").toString();
                List<String> fieldDest = new ArrayList<String>();
                fieldDest.add(copyField.get("dest").toString());
                LOGGER.debug("remove SOLR CopyField " + fieldSrc + " --> " + fieldDest.get(0));
                SchemaRequest.DeleteCopyField delCopyField = new SchemaRequest.DeleteCopyField(fieldSrc, fieldDest);
                delCopyField.process(solrClient);
            }
            LOGGER.debug("CopyFields cleaned in core " + coreName);

            SchemaRequest.Fields fieldsReq = new SchemaRequest.Fields();
            for (Map<String, Object> field : fieldsReq.process(solrClient).getFields()) {
                String fieldName = field.get("name").toString();
                if (!SOLR_DEFAULT_FIELDS.contains(fieldName)) {
                    LOGGER.debug("remove SOLR Field " + fieldName);
                    SchemaRequest.DeleteField delField = new SchemaRequest.DeleteField(fieldName);
                    delField.process(solrClient);
                }
            }
            LOGGER.debug("Fields cleaned in core " + coreName);

            SchemaRequest.DynamicFields dynFieldsReq = new SchemaRequest.DynamicFields();
            for (Map<String, Object> field : dynFieldsReq.process(solrClient).getDynamicFields()) {
                String fieldName = field.get("name").toString();
                if (!SOLR_DEFAULT_DYNAMIC_FIELDS.contains(fieldName)) {
                    LOGGER.debug("remove SOLR DynamicField " + fieldName);
                    SchemaRequest.DeleteDynamicField delField = new SchemaRequest.DeleteDynamicField(fieldName);
                    delField.process(solrClient);
                }
            }
            LOGGER.debug("DynamicFields cleaned in core " + coreName);

            SchemaRequest.FieldTypes fieldTypesReq = new SchemaRequest.FieldTypes();
            for (FieldTypeRepresentation fieldType : fieldTypesReq.process(solrClient).getFieldTypes()) {
                String fieldTypeName = fieldType.getAttributes().get("name").toString();
                if (!SOLR_DEFAULT_FIELDTYPES.contains(fieldTypeName)) {
                    LOGGER.debug("remove SOLR FieldType " + fieldTypeName);
                    SchemaRequest.DeleteFieldType delField = new SchemaRequest.DeleteFieldType(fieldTypeName);
                    delField.process(solrClient);
                }
            }
            LOGGER.debug("FieldTypes cleaned in core " + coreName);

        } catch (IOException | SolrServerException e) {
            LOGGER.error(e);
        }
    }

    /**
     * This method modified the SOLR schema definition based on all solr/{coreType}/solr-schema.json 
     * in the MyCoRe-Maven modules resource path.
     * 
     * @param coreName the name of the SOLR core
     * @param coreType the type string of the core, use <b>default-core</b> for the MyCoRe default application core
     */
    public static void processSchemaFiles(String coreName, String coreType) {
        LOGGER.info("Load schema definitions for core type " + coreType + " in core " + coreName);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            Enumeration<? extends InputStream> files = MCRConfigurationInputStream.getConfigFileInputStreams(
                "solr/" + coreType + "/" + SOLR_SCHEMA_UPDATE_FILE_NAME, null);
            while (files.hasMoreElements()) {
                try (InputStream is = files.nextElement()) {
                    String content = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);

                    JsonParser parser = new JsonParser();
                    JsonElement json = parser.parse(content);
                    if (!json.isJsonArray()) {
                        JsonElement e = json;
                        json = new JsonArray();
                        json.getAsJsonArray().add(e);
                    }

                    for (JsonElement e : json.getAsJsonArray()) {

                        LOGGER.debug(e.toString());

                        HttpPost post = new HttpPost(SOLR_SERVER_URL + coreName + "/schema");
                        post.setHeader("Content-type", "application/json");
                        post.setEntity(new StringEntity(e.toString()));

                        CloseableHttpResponse response = httpClient.execute(post);
                        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                            String respContent = new String(ByteStreams.toByteArray(response.getEntity().getContent()),
                                StandardCharsets.UTF_8);
                            LOGGER.info("SOLR schema update successful \n" + respContent);
                        } else {

                            String respContent = new String(ByteStreams.toByteArray(response.getEntity().getContent()),
                                StandardCharsets.UTF_8);
                            LOGGER.error("SOLR schema update error: " + response.getStatusLine().getStatusCode() + " "
                                + response.getStatusLine().getReasonPhrase() + "\n" + respContent);
                        }
                    }
                }
            }

        } catch (IOException e) {
            LOGGER.error(e);
        }
    }
}
