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

package org.mycore.pi.doi.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.mycore.pi.doi.MCRDOIParser;
import org.mycore.pi.doi.MCRDigitalObjectIdentifier;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MCRDOIRest {

    private static final String URL_TEMPLATE = "http://doi.org/api/handles/{doi}";

    private static CloseableHttpClient getHttpClient() {
        return HttpClientBuilder.create().build();
    }

    public static MCRDOIRestResponse get(MCRDigitalObjectIdentifier doi) throws MCRIdentifierUnresolvableException {

        HttpGet get = new HttpGet(URL_TEMPLATE.replaceAll("\\{doi\\}", doi.asString()));
        try (CloseableHttpClient httpClient = getHttpClient()) {
            CloseableHttpResponse response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();

            try (BufferedReader buffer = new BufferedReader(
                new InputStreamReader(entity.getContent(), Charset.forName("UTF-8")))) {
                String json = buffer.lines().collect(Collectors.joining("\n"));
                Gson gson = new GsonBuilder().registerTypeAdapter(MCRDOIRestResponseEntryData.class,
                    new MCRDOIRestResponseEntryDataValueDeserializer()).create();
                return gson.fromJson(json, MCRDOIRestResponse.class);
            }

        } catch (IOException e) {
            throw new MCRIdentifierUnresolvableException(doi.asString(),
                "The identifier " + doi.asString() + " is not resolvable!", e);
        }

    }

    public static void main(String[] args) throws MCRIdentifierUnresolvableException {
        MCRDOIRestResponse mcrdoiRestResponse = get(new MCRDOIParser().parse("10.1000/1").get());
    }

}
