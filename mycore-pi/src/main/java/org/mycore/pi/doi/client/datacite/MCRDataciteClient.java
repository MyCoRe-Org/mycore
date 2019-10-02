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

package org.mycore.pi.doi.client.datacite;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.pi.doi.MCRDOIParser;
import org.mycore.pi.doi.MCRDigitalObjectIdentifier;
import org.mycore.pi.exceptions.MCRDatacenterAuthenticationException;
import org.mycore.pi.exceptions.MCRDatacenterException;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

/**
 * Used for DOI registration.
 * <ol>
 * <li>use {@link #storeMetadata(Document)} to store a datacite document (should include a {@link MCRDigitalObjectIdentifier})</li>
 * <li>use {@link #mintDOI(MCRDigitalObjectIdentifier, URI)} to "register" the {@link MCRDigitalObjectIdentifier} with a URI</li>
 * <li>use {@link #setMediaList(MCRDigitalObjectIdentifier, List)} to add a list of mime-type URI pairs to a DOI</li>
 * </ol>
 */
public class MCRDataciteClient {

    public static final String DOI_REGISTER_REQUEST_TEMPLATE = "doi=%s\nurl=%s";

    private static final String HTTPS_SCHEME = "https";

    private static final Logger LOGGER = LogManager.getLogger();

    private String host;

    private String userName;

    private String password;

    /**
     * @param host       the host (in most cases mds.datacite.org)
     * @param userName   the login username will be used in every method or null if no login should be used
     * @param password   the password
     */
    public MCRDataciteClient(String host, String userName, String password) {
        this.host = host;
        this.userName = userName;
        this.password = password;
    }

    private static byte[] documentToByteArray(Document document) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        xout.output(document, outputStream);

        return outputStream.toByteArray();
    }

    private static String getStatusString(final HttpResponse resp) throws IOException {
        StatusLine statusLine = resp.getStatusLine();
        StringBuilder statusStringBuilder = new StringBuilder();

        statusStringBuilder.append(statusLine.getStatusCode()).append(" - ").append(statusLine.getReasonPhrase())
            .append(" - ");

        try (final Scanner scanner = new Scanner(resp.getEntity().getContent(), "UTF-8")) {
            while (scanner.hasNextLine()) {
                statusStringBuilder.append(scanner.nextLine());
            }
        }

        return statusStringBuilder.toString();
    }

    public List<Map.Entry<String, URI>> getMediaList(final MCRDigitalObjectIdentifier doi)
        throws MCRPersistentIdentifierException {
        ArrayList<Map.Entry<String, URI>> entries = new ArrayList<>();

        URI requestURI = getRequestURI("/media/" + doi.asString());
        HttpGet httpGet = new HttpGet(requestURI);

        try (CloseableHttpClient httpClient = getHttpClient()) {
            CloseableHttpResponse response = httpClient.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            switch (statusLine.getStatusCode()) {
                case HttpStatus.SC_OK:
                    try (final Scanner scanner = new Scanner(response.getEntity().getContent(), "UTF-8")) {
                        while (scanner.hasNextLine()) {
                            String line = scanner.nextLine();
                            String[] parts = line.split("=", 2);
                            String mediaType = parts[0];
                            URI mediaURI = new URI(parts[1]);
                            entries.add(new AbstractMap.SimpleEntry<>(mediaType, mediaURI));
                        }
                        return entries;
                    }
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new MCRDatacenterAuthenticationException();
                case HttpStatus.SC_NOT_FOUND:
                    throw new MCRIdentifierUnresolvableException(doi.asString(),
                        doi.asString() + " is not resolvable! " + getStatusString(response));
                    // return entries; // datacite says no media attached or doi does not exist (not sure what to do)
                default:
                    throw new MCRDatacenterException(
                        String.format(Locale.ENGLISH, "Datacenter-Error while set media-list for doi: \"%s\" : %s",
                            doi.asString(), getStatusString(response)));
            }
        } catch (IOException e) {
            throw new MCRDatacenterException("Unknown error while set media list", e);
        } catch (URISyntaxException e) {
            throw new MCRDatacenterException("Could not parse media url!", e);
        }
    }

    public void setMediaList(final MCRDigitalObjectIdentifier doi, List<Map.Entry<String, URI>> mediaList)
        throws MCRPersistentIdentifierException {
        URI requestURI = getRequestURI("/media/" + doi.asString());

        HttpPost post = new HttpPost(requestURI);

        String requestBodyString = mediaList.stream()
            .map(buildPair())
            .collect(Collectors.joining("\r\n"));

        LOGGER.info(requestBodyString);

        StringEntity requestEntity = new StringEntity(requestBodyString, ContentType.create("text/plain", "UTF-8"));
        post.setEntity(requestEntity);
        try (CloseableHttpClient httpClient = getHttpClient()) {
            CloseableHttpResponse response = httpClient.execute(post);
            StatusLine statusLine = response.getStatusLine();

            switch (statusLine.getStatusCode()) {
                case HttpStatus.SC_OK:
                    return;
                case HttpStatus.SC_BAD_REQUEST:
                    throw new MCRDatacenterException(
                        getStatusString(response)); // non-supported mime-type, not allowed URL domain
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new MCRDatacenterAuthenticationException();
                default:
                    throw new MCRDatacenterException(
                        String.format(Locale.ENGLISH, "Datacenter-Error while set media-list for doi: \"%s\" : %s",
                            doi.asString(), getStatusString(response)));
            }
        } catch (IOException e) {
            throw new MCRDatacenterException("Unknown error while set media list", e);
        }
    }

    private Function<Map.Entry<String, URI>, String> buildPair() {
        return entry -> entry.getKey() + "=" + entry.getValue();
    }

    public void mintDOI(final MCRDigitalObjectIdentifier doi, URI url) throws MCRPersistentIdentifierException {
        URI requestURI = getRequestURI("/doi");

        HttpPost post = new HttpPost(requestURI);

        try (CloseableHttpClient httpClient = getHttpClient()) {
            post.setEntity(new StringEntity(
                String.format(Locale.ENGLISH, DOI_REGISTER_REQUEST_TEMPLATE, doi.asString(), url.toString())));
            CloseableHttpResponse response = httpClient.execute(post);
            StatusLine statusLine = response.getStatusLine();
            switch (statusLine.getStatusCode()) {
                case HttpStatus.SC_CREATED:
                    return;
                case HttpStatus.SC_BAD_REQUEST:
                    throw new MCRDatacenterException(
                        getStatusString(response)); // invalid PREFIX or wrong format, but format is hard defined!
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new MCRDatacenterAuthenticationException();
                case HttpStatus.SC_PRECONDITION_FAILED:
                    throw new MCRDatacenterException(String.format(Locale.ENGLISH,
                        "Metadata must be uploaded first! (%s)", getStatusString(response)));
                default:
                    throw new MCRDatacenterException(String.format(Locale.ENGLISH,
                        "Datacenter-Error while minting doi: \"%s\" : %s", doi.asString(), getStatusString(response)));
            }
        } catch (IOException e) {
            throw new MCRDatacenterException("Unknown error while mint new doi", e);
        }
    }

    public List<MCRDigitalObjectIdentifier> getDOIList() throws MCRPersistentIdentifierException {
        URI requestURI = getRequestURI("/doi");

        HttpGet get = new HttpGet(requestURI);
        try (CloseableHttpClient httpClient = getHttpClient()) {
            CloseableHttpResponse response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();
            StatusLine statusLine = response.getStatusLine();
            switch (statusLine.getStatusCode()) {
                case HttpStatus.SC_OK:
                    try (final Scanner scanner = new Scanner(entity.getContent(), "UTF-8")) {
                        List<MCRDigitalObjectIdentifier> doiList = new ArrayList<>();
                        while (scanner.hasNextLine()) {
                            String line = scanner.nextLine();
                            Optional<MCRDigitalObjectIdentifier> parse = new MCRDOIParser().parse(line);
                            MCRDigitalObjectIdentifier doi = parse
                                .orElseThrow(() -> new MCRException("Could not parse DOI from Datacite!"));
                            doiList.add(doi);
                        }
                        return doiList;
                    }
                case HttpStatus.SC_NO_CONTENT:
                    return Collections.emptyList();
                default:
                    throw new MCRDatacenterException(
                        String.format(Locale.ENGLISH, "Unknown error while resolving all doi’s \n %d - %s",
                            statusLine.getStatusCode(), statusLine.getReasonPhrase()));
            }
        } catch (IOException e) {
            throw new MCRDatacenterException("Unknown error while resolving all doi’s", e);
        }
    }

    public URI resolveDOI(final MCRDigitalObjectIdentifier doiParam) throws MCRPersistentIdentifierException {

        URI requestURI = getRequestURI("/doi/" + doiParam.asString());
        HttpGet get = new HttpGet(requestURI);
        try (CloseableHttpClient httpClient = getHttpClient()) {
            CloseableHttpResponse response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();
            StatusLine statusLine = response.getStatusLine();
            switch (statusLine.getStatusCode()) {
                case HttpStatus.SC_OK:
                    try(final Scanner scanner = new Scanner(entity.getContent(), "UTF-8")){
                        String uriString = scanner.nextLine();
                        return new URI(uriString);
                    }
                case HttpStatus.SC_NO_CONTENT:
                    throw new MCRIdentifierUnresolvableException(doiParam.asString(),
                        "The identifier " + doiParam.asString() + " is currently not resolvable");
                case HttpStatus.SC_NOT_FOUND:
                    throw new MCRIdentifierUnresolvableException(doiParam.asString(),
                        "The identifier " + doiParam.asString() + " was not found in the Datacenter!");
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new MCRDatacenterAuthenticationException();
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    throw new MCRDatacenterException(
                        String.format(Locale.ENGLISH, "Datacenter error while resolving doi: \"%s\" : %s",
                            doiParam.asString(), getStatusString(response)));
                default:
                    throw new MCRDatacenterException(String.format(Locale.ENGLISH,
                        "Unknown error while resolving doi: \"%s\" : %s", doiParam.asString(),
                        getStatusString(response)));
            }
        } catch (IOException | URISyntaxException ex) {
            throw new MCRDatacenterException(
                String.format(Locale.ENGLISH, "Unknown error while resolving doi: \"%s\"", doiParam.asString()), ex);
        }
    }

    private URI getRequestURI(String path) {
        try {
            URIBuilder builder = new URIBuilder()
                .setScheme(HTTPS_SCHEME)
                .setHost(this.host)
                .setPath(path);

            return builder.build();
        } catch (URISyntaxException e) {
            throw new MCRConfigurationException("Invalid URI Exception!", e);
        }
    }

    /**
     *
     * @param doi the doi
     * @return the resolved metadata of the doi
     * @throws MCRDatacenterAuthenticationException if the authentication is wrong
     * @throws MCRIdentifierUnresolvableException if the doi is not valid or can not be resolved
     * @throws JDOMException if the metadata is empty or not a valid xml document
     * @throws MCRDatacenterException if there is something wrong with the communication with the datacenter
     */
    public Document resolveMetadata(final MCRDigitalObjectIdentifier doi) throws MCRDatacenterAuthenticationException, MCRIdentifierUnresolvableException, JDOMException, MCRDatacenterException {
        URI requestURI = getRequestURI("/metadata/" + doi.asString());
        HttpGet get = new HttpGet(requestURI);
        try (CloseableHttpClient httpClient = getHttpClient()) {
            CloseableHttpResponse response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();
            StatusLine statusLine = response.getStatusLine();
            switch (statusLine.getStatusCode()) {
                case HttpStatus.SC_OK:
                    SAXBuilder builder = new SAXBuilder();
                    return builder.build(entity.getContent());
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new MCRDatacenterAuthenticationException();
                case HttpStatus.SC_NO_CONTENT:
                    throw new MCRIdentifierUnresolvableException(doi.asString(),
                        "The identifier " + doi.asString() + " is currently not resolvable");
                case HttpStatus.SC_NOT_FOUND:
                    throw new MCRIdentifierUnresolvableException(doi.asString(),
                        "The identifier " + doi.asString() + " was not found!");
                case HttpStatus.SC_GONE:
                    throw new MCRIdentifierUnresolvableException(doi.asString(),
                        "The identifier " + doi.asString() + " was deleted!");
                default:
                    throw new MCRDatacenterException("Unknown return status: " + getStatusString(response));
            }
        } catch (IOException e) {
            throw new MCRDatacenterException("Error while resolving metadata!", e);
        }
    }

    public URI storeMetadata(Document metadata) throws MCRPersistentIdentifierException {
        URI requestURI = getRequestURI("/metadata");

        HttpPost post = new HttpPost(requestURI);
        try (CloseableHttpClient httpClient = getHttpClient()) {
            byte[] documentBytes = documentToByteArray(metadata);
            ByteArrayEntity inputStreamEntity = new ByteArrayEntity(documentBytes,
                ContentType.create("application/xml", "UTF-8"));
            post.setEntity(inputStreamEntity);
            CloseableHttpResponse response = httpClient.execute(post);
            StatusLine statusLine = response.getStatusLine();

            StringBuilder sb = new StringBuilder();
            try (InputStream is = response.getEntity().getContent()) {
                Scanner scanner = new Scanner(is, StandardCharsets.UTF_8);
                while (scanner.hasNextLine()) {
                    sb.append(scanner.nextLine()).append(System.lineSeparator());
                }
            } catch (IOException | UnsupportedOperationException e) {
                LOGGER.warn("Could not read content!", e);
            }
            String responseString = sb.toString();

            switch (statusLine.getStatusCode()) {
                case HttpStatus.SC_CREATED:
                    Header[] responseHeaders = response.getAllHeaders();
                    for (Header responseHeader : responseHeaders) {
                        if (responseHeader.getName().equals("Location")) {
                            return new URI(responseHeader.getValue());
                        }
                    }
                    // should not happen
                    throw new MCRDatacenterException("Location header not found in response! - " + responseString);
                case HttpStatus.SC_BAD_REQUEST: // invalid xml or wrong PREFIX
                    throw new MCRDatacenterException("Invalid xml or wrong PREFIX: " + statusLine.getStatusCode()
                        + " - " + statusLine.getReasonPhrase() + " - " + responseString);
                case HttpStatus.SC_UNAUTHORIZED: // no login
                    throw new MCRDatacenterAuthenticationException();
                default:
                    throw new MCRDatacenterException("Unknown return status: " + statusLine.getStatusCode() + " - "
                        + statusLine.getReasonPhrase() + " - " + responseString);
            }
        } catch (IOException | URISyntaxException e) {
            throw new MCRDatacenterException("Error while storing metadata!", e);
        }
    }

    public void deleteMetadata(final MCRDigitalObjectIdentifier doi) throws MCRPersistentIdentifierException {
        URI requestURI = getRequestURI("/metadata/" + doi.asString());

        HttpDelete delete = new HttpDelete(requestURI);
        try (CloseableHttpClient httpClient = getHttpClient()) {
            CloseableHttpResponse response = httpClient.execute(delete);
            StatusLine statusLine = response.getStatusLine();

            switch (statusLine.getStatusCode()) {
                case HttpStatus.SC_OK:
                    return;
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new MCRDatacenterAuthenticationException();
                case HttpStatus.SC_NOT_FOUND:
                    throw new MCRIdentifierUnresolvableException(doi.asString(), doi.asString() + " was not found!");
                default:
                    throw new MCRDatacenterException(
                        "Unknown return status: " + statusLine.getStatusCode() + " - " + statusLine.getReasonPhrase());
            }
        } catch (IOException e) {
            throw new MCRDatacenterException("Error while deleting metadata!", e);
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
