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

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.mycore.pi.util.MCRHttpUtils;
import org.mycore.pi.util.MCRResultOrException;

import com.google.gson.Gson;

/**
 * Implementation for this api <a href="https://doc.pidconsortium.eu/docs/">https://doc.pidconsortium.eu/docs/</a>
 */
public class MCREpicClient {
    private static final AuthScope ANY_AUTHSCOPE = new AuthScope(null, -1);

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
        try (CloseableHttpClient httpClient = getHttpClient()) {
            httpClient.<MCRResultOrException<Object, MCREpicException>>execute(httpDelete,
                response -> switch (response.getCode()) {
                    case HttpStatus.SC_NO_CONTENT -> MCRResultOrException.ofResult(null);
                    case HttpStatus.SC_UNAUTHORIZED
                        -> MCRResultOrException.ofException(new MCREpicUnauthorizedException(
                            "Error while create:" + response.getReasonPhrase()));
                    default -> {
                        final String content = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                        yield MCRResultOrException
                            .ofException(new MCREpicException("Unknown error: " + response.getCode() + " - "
                                + response.getReasonPhrase() + " - " + content));
                    }
                }).getResultOrThrow();
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

        try (CloseableHttpClient httpClient = getHttpClient()) {

            httpClient.<MCRResultOrException<Object, MCREpicException>>execute(httpPut,
                response -> switch (response.getCode()) {
                    case HttpStatus.SC_CREATED, HttpStatus.SC_NO_CONTENT -> MCRResultOrException.ofResult(null);
                    case HttpStatus.SC_PRECONDITION_FAILED -> MCRResultOrException.ofException(
                        new MCREpicException("The Precondition failed, which means the handle already exist!"));
                    case HttpStatus.SC_UNAUTHORIZED
                        -> MCRResultOrException.ofException(
                            new MCREpicUnauthorizedException("Error while create:" + response.getReasonPhrase()));
                    default -> {
                        final String content = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                        yield MCRResultOrException.ofException(
                            new MCREpicException("Unknown error: " + response.getCode() + " - "
                                + response.getReasonPhrase() + " - " + content));
                    }
                }).getResultOrThrow();
        }
    }

    public List<MCRHandleInfo> get(MCRHandle hdl) throws IOException, MCREpicException {
        try (CloseableHttpClient httpClient = getHttpClient()) {

            return httpClient.<MCRResultOrException<List<MCRHandleInfo>, MCREpicException>>execute(
                new HttpGet(baseURL + "handles/" + hdl.toString()),
                response -> switch (response.getCode()) {
                    case HttpStatus.SC_OK -> {
                        try (InputStream content = response.getEntity().getContent();
                            Reader reader = new InputStreamReader(content, StandardCharsets.UTF_8)) {
                            final Gson gson = new Gson();

                            final MCRHandleInfo[] handleInfos = gson.fromJson(reader, MCRHandleInfo[].class);
                            yield MCRResultOrException.ofResult(Arrays.asList(handleInfos));
                        }
                    }
                    case HttpStatus.SC_UNAUTHORIZED -> MCRResultOrException.ofException(
                        new MCREpicUnauthorizedException("Error while listIds:" + response.getReasonPhrase()));
                    default -> MCRResultOrException.ofException(
                        new MCREpicException("Error while listIds" + response.getReasonPhrase()));
                }).getResultOrThrow();
        }
    }

    public List<MCRHandle> listIds(String prefix) throws IOException, MCREpicException {
        try (CloseableHttpClient httpClient = getHttpClient()) {
            return httpClient.<MCRResultOrException<List<MCRHandle>, MCREpicException>>execute(
                new HttpGet(baseURL + "handles/" + prefix + "/"), response -> {
                    final String prefix2 = prefix + "/";
                    HttpEntity entity = response.getEntity();
                    return switch (response.getCode()) {
                        case HttpStatus.SC_OK -> {
                            try (InputStream content = entity.getContent();
                                InputStreamReader inputStreamReader =
                                    new InputStreamReader(content, StandardCharsets.UTF_8);
                                BufferedReader br = new BufferedReader(inputStreamReader)) {
                                yield MCRResultOrException.ofResult(
                                    br.lines().map(prefix2::concat).map(MCRHandle::new).collect(Collectors.toList()));
                            }
                        }
                        case HttpStatus.SC_UNAUTHORIZED -> MCRResultOrException.ofException(
                            new MCREpicUnauthorizedException("Error while listIds:" + response.getReasonPhrase()));
                        default -> MCRResultOrException.ofException(
                            new MCREpicException("Error while listIds" + response.getReasonPhrase()));
                    };
                }).getResultOrThrow();
        }
    }

    private CloseableHttpClient getHttpClient() {
        return MCRHttpUtils.getHttpClient().setDefaultCredentialsProvider(getCredentialsProvider()).build();
    }

    private BasicCredentialsProvider getCredentialsProvider() {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        if (userName != null && !userName.isEmpty() && password != null && !password.isEmpty()) {
            credentialsProvider.setCredentials(ANY_AUTHSCOPE, getCredentials());
        }

        return credentialsProvider;
    }

    private UsernamePasswordCredentials getCredentials() {
        return new UsernamePasswordCredentials(this.userName, this.password.toCharArray());
    }
}
