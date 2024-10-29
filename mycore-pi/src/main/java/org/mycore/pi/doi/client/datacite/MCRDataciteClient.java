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

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
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
import org.mycore.pi.util.MCRResultOrException;
import org.mycore.services.http.MCRHttpUtils;

/**
 * Used for DOI registration.
 * <ol>
 * <li>use {@link #storeMetadata(Document)} to store a datacite document
 * (should include a {@link MCRDigitalObjectIdentifier})</li>
 * <li>use {@link #mintDOI(MCRDigitalObjectIdentifier, URI)} to "register"
 * the {@link MCRDigitalObjectIdentifier} with a URI</li>
 * <li>use {@link #setMediaList(MCRDigitalObjectIdentifier, List)} to add a list of mime-type URI pairs to a DOI</li>
 * </ol>
 */
public class MCRDataciteClient {

    public static final String DOI_REGISTER_REQUEST_TEMPLATE = "doi=%s\nurl=%s";

    private static final String HTTPS_SCHEME = "https";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final AuthScope ANY_AUTHSCOPE = new AuthScope(null, -1);

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

    private static String getStatusString(final ClassicHttpResponse resp) throws IOException {
        StringBuilder statusStringBuilder = new StringBuilder();

        statusStringBuilder.append(resp.getCode()).append(" - ").append(resp.getReasonPhrase())
            .append(" - ");

        try (InputStream content = resp.getEntity().getContent();
            Scanner scanner = new Scanner(content, StandardCharsets.UTF_8)) {
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
            MCRResultOrException<List<Map.Entry<String, URI>>, MCRPersistentIdentifierException> result
                = httpClient.execute(httpGet, response -> {
                    return switch (response.getCode()) {
                        case HttpStatus.SC_OK -> {
                            try (InputStream content = response.getEntity().getContent();
                                Scanner scanner = new Scanner(content, StandardCharsets.UTF_8)) {
                                while (scanner.hasNextLine()) {
                                    String line = scanner.nextLine();
                                    String[] parts = line.split("=", 2);
                                    String mediaType = parts[0];
                                    URI mediaURI = new URI(parts[1]);
                                    entries.add(new AbstractMap.SimpleEntry<>(mediaType, mediaURI));
                                }
                                yield MCRResultOrException.ofResult(entries);
                            } catch (URISyntaxException e) {
                                yield MCRResultOrException
                                    .ofException(new MCRDatacenterException("Could not parse media url!", e));
                            }
                        }
                        case HttpStatus.SC_UNAUTHORIZED
                            -> MCRResultOrException.ofException(new MCRDatacenterAuthenticationException());
                        case HttpStatus.SC_NOT_FOUND
                            -> MCRResultOrException.ofException(new MCRIdentifierUnresolvableException(doi.asString(),
                                doi.asString() + " is not resolvable! " + getStatusString(response)));

                        // datacite says no media attached or doi does not exist (not sure what to do)
                        default -> MCRResultOrException.ofException(new MCRDatacenterException(
                            String.format(Locale.ENGLISH, "Datacenter-Error while set media-list for doi: \"%s\" : %s",
                                doi.asString(), getStatusString(response))));
                    };
                });
            return result.getResultOrThrow();
        } catch (IOException e) {
            throw new MCRDatacenterException("Unknown error while set media list", e);
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
            httpClient.<MCRResultOrException<Object, MCRPersistentIdentifierException>>execute(post, response -> {
                return switch (response.getCode()) {
                    case HttpStatus.SC_OK -> MCRResultOrException.ofResult(null);
                    case HttpStatus.SC_BAD_REQUEST -> MCRResultOrException.ofException(new MCRDatacenterException(
                        getStatusString(response))); // non-supported mime-type, not allowed URL domain
                    case HttpStatus.SC_UNAUTHORIZED
                        -> MCRResultOrException.ofException(new MCRDatacenterAuthenticationException());
                    default -> MCRResultOrException.ofException(new MCRDatacenterException(
                        String.format(Locale.ENGLISH, "Datacenter-Error while set media-list for doi: \"%s\" : %s",
                            doi.asString(), getStatusString(response))));
                };
            }).getResultOrThrow();

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
                String.format(Locale.ROOT, DOI_REGISTER_REQUEST_TEMPLATE, doi.asString(), url.toString())));
            httpClient.<MCRResultOrException<Object, MCRPersistentIdentifierException>>execute(post,
                response -> switch (response.getCode()) {
                    case HttpStatus.SC_CREATED -> MCRResultOrException.ofResult(null);
                    case HttpStatus.SC_BAD_REQUEST -> MCRResultOrException.ofException(new MCRDatacenterException(
                        getStatusString(response))); // invalid PREFIX or wrong format, but format is hard defined!
                    case HttpStatus.SC_UNAUTHORIZED
                        -> MCRResultOrException.ofException(new MCRDatacenterAuthenticationException());
                    case HttpStatus.SC_PRECONDITION_FAILED
                        -> MCRResultOrException.ofException(new MCRDatacenterException(String.format(Locale.ENGLISH,
                            "Metadata must be uploaded first! (%s)", getStatusString(response))));
                    default -> MCRResultOrException.ofException(new MCRDatacenterException(String.format(Locale.ENGLISH,
                        "Datacenter-Error while minting doi: \"%s\" : %s", doi.asString(), getStatusString(response))));

                }).getResultOrThrow();
        } catch (IOException e) {
            throw new MCRDatacenterException("Unknown error while mint new doi", e);
        }
    }

    public List<MCRDigitalObjectIdentifier> getDOIList() throws MCRPersistentIdentifierException {
        URI requestURI = getRequestURI("/doi");

        HttpGet get = new HttpGet(requestURI);
        try (CloseableHttpClient httpClient = getHttpClient()) {
            MCRResultOrException<List<MCRDigitalObjectIdentifier>, MCRPersistentIdentifierException> result
                = httpClient.execute(get, response -> {
                    HttpEntity entity = response.getEntity();
                    return switch (response.getCode()) {
                        case HttpStatus.SC_OK -> {
                            try (InputStream content = entity.getContent();
                                Scanner scanner = new Scanner(content, StandardCharsets.UTF_8)) {
                                List<MCRDigitalObjectIdentifier> doiList = new ArrayList<>();
                                while (scanner.hasNextLine()) {
                                    String line = scanner.nextLine();
                                    Optional<MCRDigitalObjectIdentifier> parse = new MCRDOIParser().parse(line);
                                    MCRDigitalObjectIdentifier doi = parse
                                        .orElseThrow(() -> new MCRException("Could not parse DOI from Datacite!"));
                                    doiList.add(doi);
                                }
                                yield MCRResultOrException.ofResult(doiList);
                            }
                        }
                        case HttpStatus.SC_NO_CONTENT -> MCRResultOrException.ofResult(Collections.emptyList());
                        default -> MCRResultOrException.ofException(new MCRDatacenterException(
                            String.format(Locale.ENGLISH, "Unknown error while resolving all doi’s \n %d - %s",
                                response.getCode(), response.getReasonPhrase())));
                    };
                });
            return result.getResultOrThrow();
        } catch (IOException e) {
            throw new MCRDatacenterException("Unknown error while resolving all doi’s", e);
        }
    }

    public URI resolveDOI(final MCRDigitalObjectIdentifier doiParam) throws MCRPersistentIdentifierException {

        URI requestURI = getRequestURI("/doi/" + doiParam.asString());
        HttpGet get = new HttpGet(requestURI);
        try (CloseableHttpClient httpClient = getHttpClient()) {
            MCRResultOrException<URI, MCRPersistentIdentifierException> result = httpClient.execute(get, response -> {
                HttpEntity entity = response.getEntity();
                return switch (response.getCode()) {
                    case HttpStatus.SC_OK -> {
                        String uriString = null;
                        try (InputStream content = entity.getContent();
                            Scanner scanner = new Scanner(content, StandardCharsets.UTF_8)) {
                            uriString = scanner.nextLine();
                            yield MCRResultOrException.ofResult(new URI(uriString));
                        } catch (URISyntaxException e) {
                            yield MCRResultOrException
                                .ofException(new MCRDatacenterException(String.format(Locale.ENGLISH,
                                    "Unknown error while resolving doi: \"%s\"", doiParam.asString()), e));
                        }
                    }
                    case HttpStatus.SC_NO_CONTENT
                        -> MCRResultOrException.ofException(new MCRIdentifierUnresolvableException(doiParam.asString(),
                            "The identifier " + doiParam.asString() + " is currently not resolvable"));
                    case HttpStatus.SC_NOT_FOUND
                        -> MCRResultOrException.ofException(new MCRIdentifierUnresolvableException(doiParam.asString(),
                            "The identifier " + doiParam.asString() + " was not found in the Datacenter!"));
                    case HttpStatus.SC_UNAUTHORIZED
                        -> MCRResultOrException.ofException(new MCRDatacenterAuthenticationException());
                    case HttpStatus.SC_INTERNAL_SERVER_ERROR
                        -> MCRResultOrException.ofException(new MCRDatacenterException(
                            String.format(Locale.ENGLISH, "Datacenter error while resolving doi: \"%s\" : %s",
                                doiParam.asString(), getStatusString(response))));
                    default -> MCRResultOrException.ofException(new MCRDatacenterException(String.format(Locale.ENGLISH,
                        "Unknown error while resolving doi: \"%s\" : %s", doiParam.asString(),
                        getStatusString(response))));
                };
            });
            return result.getResultOrThrow();
        } catch (IOException ex) {
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
    public Document resolveMetadata(final MCRDigitalObjectIdentifier doi) throws MCRDatacenterAuthenticationException,
        MCRIdentifierUnresolvableException, JDOMException, MCRDatacenterException {
        URI requestURI = getRequestURI("/metadata/" + doi.asString());
        HttpGet get = new HttpGet(requestURI);
        try (CloseableHttpClient httpClient = getHttpClient()) {
            MCRResultOrException<Document, Exception> resultOrException = httpClient.execute(get, response -> {
                HttpEntity entity = response.getEntity();
                if (entity != null && response.getCode() != HttpStatus.SC_OK) {
                    EntityUtils.consume(entity);
                }
                return switch (response.getCode()) {
                    case HttpStatus.SC_OK -> {
                        SAXBuilder builder = new SAXBuilder();
                        try (InputStream content = entity.getContent();) {
                            Document result = builder.build(content);
                            yield MCRResultOrException.ofResult(result);
                        } catch (JDOMException e) {
                            yield MCRResultOrException.ofException(e);
                        }
                    }
                    case HttpStatus.SC_UNAUTHORIZED
                        -> MCRResultOrException.ofException(new MCRDatacenterAuthenticationException());
                    case HttpStatus.SC_NO_CONTENT
                        -> MCRResultOrException.ofException(new MCRIdentifierUnresolvableException(doi.asString(),
                            "The identifier " + doi.asString() + " is currently not resolvable"));
                    case HttpStatus.SC_NOT_FOUND
                        -> MCRResultOrException.ofException(new MCRIdentifierUnresolvableException(doi.asString(),
                            "The identifier " + doi.asString() + " was not found!"));
                    case HttpStatus.SC_GONE
                        -> MCRResultOrException.ofException(new MCRIdentifierUnresolvableException(doi.asString(),
                            "The identifier " + doi.asString() + " was deleted!"));
                    default -> MCRResultOrException
                        .ofException(new MCRDatacenterException("Unknown return status: " + getStatusString(response)));
                };
            });
            if (resultOrException.exception() != null) {
                if (resultOrException.exception() instanceof JDOMException je) {
                    throw je;
                }
                if (resultOrException.exception() instanceof MCRDatacenterException dce) {
                    throw dce;
                }
                throw new MCRDatacenterException("Error while resolving doi: " + doi, resultOrException.exception());
            }
            return resultOrException.result();
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
            MCRResultOrException<URI, MCRDatacenterException> resultOrException = httpClient.execute(post, response -> {
                final HttpEntity entity = response.getEntity();
                String responseString = EntityUtils.toString(entity);
                return switch (response.getCode()) {
                    case HttpStatus.SC_CREATED -> {
                        Header[] responseHeaders = response.getHeaders();
                        for (Header responseHeader : responseHeaders) {
                            if (responseHeader.getName().equals("Location")) {
                                try {
                                    URI newLocation = new URI(responseHeader.getValue());
                                    yield MCRResultOrException.ofResult(newLocation);
                                } catch (URISyntaxException e) {
                                    yield MCRResultOrException
                                        .ofException(new MCRDatacenterException("Error while storing metadata.", e));
                                }
                            }
                        }
                        // should not happen
                        yield MCRResultOrException.ofException(
                            new MCRDatacenterException("Location header not found in response! - " + responseString));
                    }
                    case HttpStatus.SC_BAD_REQUEST -> // invalid xml or wrong PREFIX
                        MCRResultOrException
                            .ofException(new MCRDatacenterException("Invalid xml or wrong PREFIX: " + response.getCode()
                                + " - " + response.getReasonPhrase() + " - " + responseString));
                    case HttpStatus.SC_UNAUTHORIZED -> // no login
                        MCRResultOrException.ofException(new MCRDatacenterAuthenticationException());
                    default -> MCRResultOrException.ofException(new MCRDatacenterException(
                        "Unknown return status: " + response.getCode() + " - "
                            + response.getReasonPhrase() + " - " + responseString));
                };
            });
            return resultOrException.getResultOrThrow();

        } catch (IOException e) {
            throw new MCRDatacenterException("Error while storing metadata!", e);
        }
    }

    public void deleteMetadata(final MCRDigitalObjectIdentifier doi) throws MCRPersistentIdentifierException {
        URI requestURI = getRequestURI("/metadata/" + doi.asString());

        HttpDelete delete = new HttpDelete(requestURI);
        try (CloseableHttpClient httpClient = getHttpClient()) {
            MCRDatacenterException datacenterException = httpClient.execute(delete, response -> {
                final HttpEntity entity = response.getEntity();
                EntityUtils.consume(entity);
                return switch (response.getCode()) {
                    case HttpStatus.SC_OK -> null;
                    case HttpStatus.SC_UNAUTHORIZED -> new MCRDatacenterAuthenticationException();
                    case HttpStatus.SC_NOT_FOUND -> new MCRIdentifierUnresolvableException(doi.asString(),
                        doi.asString() + " was not found!");
                    default -> new MCRDatacenterException(
                        "Unknown return status: " + response.getCode() + " - " + response.getReasonPhrase());

                };
            });
            if (datacenterException != null) {
                throw datacenterException;
            }
        } catch (IOException e) {
            throw new MCRDatacenterException("Error while deleting metadata!", e);
        }
    }

    private CloseableHttpClient getHttpClient() {
        return HttpClientBuilder.create()
            .setUserAgent(MCRHttpUtils.getHttpUserAgent())
            .useSystemProperties()
            .setDefaultCredentialsProvider(getCredentialsProvider())
            .build();
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
