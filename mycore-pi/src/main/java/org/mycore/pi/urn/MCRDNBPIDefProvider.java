package org.mycore.pi.urn;


import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;

public class MCRDNBPIDefProvider {
    private static final String RESOLVING_URL_TEMPLATE = "https://nbn-resolving.org/resolver?identifier={urn}&verb=full&xml=on";

    public static Document get(MCRDNBURN urn) throws MCRIdentifierUnresolvableException {
        HttpGet get = new HttpGet(RESOLVING_URL_TEMPLATE.replaceAll("\\{urn\\}",urn.asString()));
        try (CloseableHttpClient httpClient = getHttpClient()) {
            CloseableHttpResponse response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();
            return new SAXBuilder().build(entity.getContent());
        } catch (IOException |JDOMException e) {
            throw new MCRIdentifierUnresolvableException(urn.asString(), "The identifier " + urn.asString() + " is not resolvable!", e);
        }
    }

    private static CloseableHttpClient getHttpClient() {
        return HttpClientBuilder.create().build();
    }

}
