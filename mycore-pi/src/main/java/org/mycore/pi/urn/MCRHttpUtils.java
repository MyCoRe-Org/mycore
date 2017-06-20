package org.mycore.pi.urn;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;

/**
 * Created by chi on 31.01.17.
 * @author Huu Chi Vu
 */
public class MCRHttpUtils {
    public static CloseableHttpClient getHttpClient() {
        return HttpClientBuilder
            .create()
            .setSSLContext(SSLContexts.createSystemDefault())
            .build();
    }
}
