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

package org.mycore.pi.doi.client.datacite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpEntity;
import org.mycore.pi.doi.MCRDOIParser;
import org.mycore.pi.doi.MCRDigitalObjectIdentifier;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;
import org.mycore.pi.util.MCRHttpUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MCRDataciteRest {

    private static final String URL_TEMPLATE = "http://doi.org/api/handles/{doi}";

    public static MCRDataciteRestResponse get(MCRDigitalObjectIdentifier doi)
        throws MCRIdentifierUnresolvableException {

        HttpGet get = new HttpGet(URL_TEMPLATE.replaceAll("\\{doi\\}", doi.asString()));
        try {
            try (CloseableHttpClient httpClient = MCRHttpUtils.getHttpClient().build()) {
                return httpClient.execute(get, response -> {
                    HttpEntity entity = response.getEntity();
                    try (InputStream is = entity.getContent();
                        InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                        BufferedReader buffer = new BufferedReader(isr)) {
                        String json = buffer.lines().collect(Collectors.joining("\n"));
                        Gson gson = new GsonBuilder().registerTypeAdapter(MCRDataciteRestResponseEntryData.class,
                            new MCRDOIRestResponseEntryDataValueDeserializer()).create();
                        return gson.fromJson(json, MCRDataciteRestResponse.class);
                    }
                });
            }
        } catch (IOException e) {
            throw new MCRIdentifierUnresolvableException(doi.asString(),
                "The identifier " + doi.asString() + " is not resolvable!", e);
        }

    }

    public static void main(String[] args) throws MCRIdentifierUnresolvableException {
        get(new MCRDOIParser().parse("10.1000/1").get());
    }

}
