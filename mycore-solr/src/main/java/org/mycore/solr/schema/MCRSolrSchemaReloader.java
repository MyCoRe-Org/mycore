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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
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
import org.mycore.common.config.MCRComponent;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.common.config.MCRConfigurationInputStream;
import org.mycore.common.config.MCRRuntimeComponentDetector;
import org.mycore.solr.MCRSolrClientFactory;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * This class provides methods to reload a SOLR schema using the SOLR schema API
 *
 * @author Robert Stephan
 */
public class MCRSolrSchemaReloader {
	private static Logger LOGGER = LogManager.getLogger(MCRSolrSchemaReloader.class);

	private static String SOLR_SCHEMA_UPDATE_FILES = "solr-schema-config.json";

	private static List<String> SOLR_DEFAULT_FIELDS = Arrays.asList("_text_", "_version_", "id");
	private static List<String> SOLR_DEFAULT_FIELDTYPES = Arrays.asList("text_general", "long", "plong", "string");

	public static void clearSchema() {

		try {
			SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
			SchemaRequest.Fields fieldsReq = new SchemaRequest.Fields();
			for (Map<String, Object> field : fieldsReq.process(solrClient).getFields()) {
				String fieldName = field.get("name").toString();
				if (!SOLR_DEFAULT_FIELDS.contains(fieldName)) {
					SchemaRequest.DeleteField delField = new SchemaRequest.DeleteField(fieldName);
					delField.process(solrClient);
				}
			}
			
			// the following code is not testet ..
			SchemaRequest.FieldTypes fieldTypesReq = new SchemaRequest.FieldTypes();
			for (FieldTypeRepresentation fieldType : fieldTypesReq.process(solrClient).getFieldTypes()) {
				String fieldTypeName = fieldType.getAttributes().get("name").toString();
				if (!SOLR_DEFAULT_FIELDTYPES.contains(fieldTypeName)) {
					SchemaRequest.DeleteFieldType delField = new SchemaRequest.DeleteFieldType(fieldTypeName);
					delField.process(solrClient);
				}
			}
			
			SchemaRequest.DynamicFields dynFieldsReq = new SchemaRequest.DynamicFields();
			for (Map<String, Object> dynField : dynFieldsReq.process(solrClient).getDynamicFields()){
				String fieldName = dynField.get("name").toString();
					SchemaRequest.DeleteDynamicField delDynField = new SchemaRequest.DeleteDynamicField(fieldName);
					delDynField.process(solrClient);
			}
			
			SchemaRequest.CopyFields copyFieldsReq = new SchemaRequest.CopyFields();
			for (Map<String, Object>copyField : copyFieldsReq.process(solrClient).getCopyFields()){
				String fieldSrc = copyField.get("source").toString();
				//check if this works ...
				List<String> fieldDest = (List<String>) copyField.get("dest");
				SchemaRequest.DeleteCopyField delCopyField = new SchemaRequest.DeleteCopyField(fieldSrc,  fieldDest);
				delCopyField.process(solrClient);
			}
		} catch (IOException | SolrServerException e) {
			LOGGER.error(e);
		}
	}

	/**
	 * extension to "standard" solr schema api syntax if the structure is a JSON
	 * array split it and send every single json object
	 */
	public static void processConfigFiles() {
		String solrServerURL = MCRConfiguration.instance().getString("MCR.Module-solr.ServerURL");
		
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			Enumeration<? extends InputStream> files = getInputStreams(SOLR_SCHEMA_UPDATE_FILES, null);
			while (files.hasMoreElements()) {
				InputStream is = files.nextElement();
				String content = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);

				JsonParser parser = new JsonParser();
				JsonElement json = parser.parse(content);
				if (!json.isJsonArray()) {
					JsonElement e = json;
					json = new JsonArray();
					json.getAsJsonArray().add(e);
				}

				for (JsonElement e : json.getAsJsonArray()) {

					HttpPost post = new HttpPost(solrServerURL + "/schema");
					post.setHeader("Content-type", "application/json");
					post.setEntity(new StringEntity(e.toString()));

					CloseableHttpResponse response = httpClient.execute(post);
					if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
						String respContent = new String(ByteStreams.toByteArray(response.getEntity().getContent()),
								StandardCharsets.UTF_8);
						LOGGER.info("SOLR Schema update successful \n" + respContent);
					} else {

						String respContent = new String(ByteStreams.toByteArray(response.getEntity().getContent()),
								StandardCharsets.UTF_8);
						LOGGER.error("SOLR Schema update error: " + response.getStatusLine().getStatusCode() + " "
								+ response.getStatusLine().getReasonPhrase() + "\n" + respContent);
					}
				}

			}

		} catch (IOException e) {
			LOGGER.error(e);
		}
	}

	/**
	 * @see MCRConfigurationInputStream for implementation details
	 */
	private static Enumeration<? extends InputStream> getInputStreams(String filename, InputStream initStream)
			throws IOException {
		LinkedList<InputStream> cList = new LinkedList<>();
		if (initStream != null) {
			cList.add(initStream);
		}
		for (MCRComponent component : MCRRuntimeComponentDetector.getAllComponents()) {
			InputStream is = component.getConfigFileStream(filename);
			if (is != null) {
				cList.add(is);
			}
		}
		InputStream propertyStream = getPropertyStream(filename);
		if (propertyStream != null) {
			cList.add(propertyStream);
		}
		File localProperties = MCRConfigurationDir.getConfigFile(filename);
		if (localProperties != null && localProperties.canRead()) {
			cList.add(new FileInputStream(localProperties));
		}
		return Collections.enumeration(cList);
	}

	/**
	 * Warum nicht einfach so?
	 */
	private static InputStream getPropertyStream(String filename) throws IOException {
		File cfgFile = new File(filename);
		if (cfgFile.canRead()) {
			return new FileInputStream(cfgFile);
		} else {
			return MCRConfigurationInputStream.class.getClassLoader().getResourceAsStream(filename);
		}
	}

}
