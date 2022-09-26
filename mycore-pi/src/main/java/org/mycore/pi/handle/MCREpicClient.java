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

package org.mycore.pi.handle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.google.gson.Gson;

/**
 * Implementation for this api https://doc.pidconsortium.eu/docs/
 */
public class MCREpicClient {

    private final String userName;

    private final String password;

    private final String baseURL; // e.g. https://epic.grnet.gr/api/v2/, http://pid.gwdg.de/

    public MCREpicClient(String userName, String password, String baseURL) {
        this.userName = userName;
        this.password = password;
        this.baseURL = baseURL;
    }

    public void delete(MCRHandle handle) throws MCREpicException, IOException {
        final HttpDelete httpDelete = new HttpDelete(baseURL + "handles/" + handle.toString());
        try (CloseableHttpClient httpClient = getHttpClient();
            CloseableHttpResponse response = httpClient.execute(httpDelete)) {

            HttpEntity entity = response.getEntity();
            StatusLine statusLine = response.getStatusLine();

            switch (statusLine.getStatusCode()) {
            case HttpStatus.SC_NO_CONTENT:
                return;
            case HttpStatus.SC_UNAUTHORIZED:
                throw new MCREpicUnauthorizedException("Error while create:" + statusLine.getReasonPhrase());
            default:
                final String content = new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);
                throw new MCREpicException("Unknown error: " + statusLine.getStatusCode() + " - "
                    + statusLine.getReasonPhrase() + " - " + content);
            }
        }
    }

    public void create(String url, MCRHandle handle, List<MCRHandleInfo> additionalInformation)
        throws IOException, MCREpicException {
        final HttpPut httpPut = new HttpPut(baseURL + "handles/" + handle.toString());

        final MCRHandleInfo register = new MCRHandleInfo();

        register.setType("URL");
        register.setData(url);

        final String handleInfoStr = new Gson()
            .toJson(Stream.concat(Stream.of(register), additionalInformation.stream()).collect(Collectors.toList()));

        httpPut.setEntity(new StringEntity(handleInfoStr));
        httpPut.setHeader("Content-Type", "application/json");

        try (CloseableHttpClient httpClient = getHttpClient();
            CloseableHttpResponse response = httpClient.execute(httpPut)) {

            HttpEntity entity = response.getEntity();
            StatusLine statusLine = response.getStatusLine();

            switch (statusLine.getStatusCode()) {
            case HttpStatus.SC_CREATED:
            case HttpStatus.SC_NO_CONTENT:
                return;
            case HttpStatus.SC_PRECONDITION_FAILED:
                throw new MCREpicException("The Precondition failed, which means the handle already exist!");
            case HttpStatus.SC_UNAUTHORIZED:
                throw new MCREpicUnauthorizedException("Error while create:" + statusLine.getReasonPhrase());
            default:
                final String content = new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);
                throw new MCREpicException("Unknown error: " + statusLine.getStatusCode() + " - "
                    + statusLine.getReasonPhrase() + " - " + content);
            }
        }
    }

    public List<MCRHandleInfo> get(MCRHandle hdl) throws IOException, MCREpicException {
        try (CloseableHttpClient httpClient = getHttpClient();
            CloseableHttpResponse response = httpClient
                .execute(new HttpGet(baseURL + "handles/" + hdl.toString()))) {

            HttpEntity entity = response.getEntity();
            StatusLine statusLine = response.getStatusLine();

            switch (statusLine.getStatusCode()) {
            case HttpStatus.SC_OK:
                try (InputStream content = entity.getContent();
                    Reader reader = new InputStreamReader(content, StandardCharsets.UTF_8)) {
                    final Gson gson = new Gson();

                    final MCRHandleInfo[] handleInfos = gson.fromJson(reader, MCRHandleInfo[].class);
                    return Arrays.asList(handleInfos);
                }
            case HttpStatus.SC_UNAUTHORIZED:
                throw new MCREpicUnauthorizedException("Error while listIds:" + statusLine.getReasonPhrase());
            default:
            case HttpStatus.SC_NOT_FOUND:
                throw new MCREpicException("Error while listIds" + statusLine.getReasonPhrase());
            }
        }
    }

    public List<MCRHandle> listIds(String prefix) throws IOException, MCREpicException {
        try (CloseableHttpClient httpClient = getHttpClient();
            CloseableHttpResponse response = httpClient
                .execute(new HttpGet(baseURL + "handles/" + prefix + "/"))) {
            final String prefix2 = prefix + "/";
            HttpEntity entity = response.getEntity();
            StatusLine statusLine = response.getStatusLine();
            switch (statusLine.getStatusCode()) {
            case HttpStatus.SC_OK:
                try (InputStream content = entity.getContent();
                    InputStreamReader inputStreamReader = new InputStreamReader(content, StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(inputStreamReader)) {
                    return br.lines().map(prefix2::concat).map(MCRHandle::new).collect(Collectors.toList());
                }
            case HttpStatus.SC_UNAUTHORIZED:
                throw new MCREpicUnauthorizedException("Error while listIds:" + statusLine.getReasonPhrase());
            default:
            case HttpStatus.SC_NOT_FOUND:
                throw new MCREpicException("Error while listIds" + statusLine.getReasonPhrase());
            }
        }
    }

    private CloseableHttpClient getHttpClient() {
        return HttpClientBuilder.create().setDefaultCredentialsProvider(getCredentialsProvider()).build();
    }

    private BasicCredentialsProvider getCredentialsProvider() {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        if (userName != null && !userName.isEmpty() && password != null && !password.isEmpty()) {
            credentialsProvider.setCredentials(AuthScope.ANY, getCredentials());
        }

        return credentialsProvider;
    }

    private UsernamePasswordCredentials getCredentials() {
        return new UsernamePasswordCredentials(this.userName, this.password);
    }
}
