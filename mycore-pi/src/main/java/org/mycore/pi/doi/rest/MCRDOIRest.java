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

    private final static String URL_TEMPLATE = "http://doi.org/api/handles/{doi}";

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
