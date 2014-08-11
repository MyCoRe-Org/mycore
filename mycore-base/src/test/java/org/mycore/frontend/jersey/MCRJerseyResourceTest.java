package org.mycore.frontend.jersey;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainer;
import com.sun.jersey.test.framework.spi.container.grizzly.web.GrizzlyWebTestContainerFactory;

public abstract class MCRJerseyResourceTest extends JerseyTest {
    public MCRJerseyResourceTest() {
        super(new GrizzlyWebTestContainerApacheClientFactory());
    }

    protected AppDescriptor configure() {
        WebAppDescriptor webAppDescriptor = getBuilder().build();
        ClientConfig clientConfig = webAppDescriptor.getClientConfig();
        Map<String, Boolean> features = clientConfig.getFeatures();
        features.put("com.sun.jersey.config.feature.DisableXmlSecurity", true);
        return webAppDescriptor;
    }

    private WebAppDescriptor.Builder getBuilder() {
        WebAppDescriptor.Builder builder = new WebAppDescriptor.Builder(getPackageName()).initParam(
                "com.sun.jersey.config.feature.DisableXmlSecurity", "true").contextPath(getContextPath());
        for (Map.Entry<String, String> initParam : getInitParams().entrySet()) {
            builder.initParam((String) initParam.getKey(), (String) initParam.getValue());
        }
        return builder;
    }

    public Map<String, String> getInitParams() {
        return new HashMap<String, String>();
    }

    public String getContextPath() {
        String contextPath = "context";
        return contextPath;
    }

    public abstract String[] getPackageName();

    public static class GrizzlyWebTestContainerApacheClient implements TestContainer {
        private TestContainer container;

        public Client getClient() {
            return ApacheHttpClient.create();
        }

        public URI getBaseUri() {
            return this.container.getBaseUri();
        }

        public void start() {
            this.container.start();
        }

        public void stop() {
            this.container.stop();
        }

        public GrizzlyWebTestContainerApacheClient(TestContainer container) {
            this.container = container;
        }
    }

    public static class GrizzlyWebTestContainerApacheClientFactory extends GrizzlyWebTestContainerFactory {
        public TestContainer create(URI baseUri, AppDescriptor ad) {
            if (!(ad instanceof WebAppDescriptor)) {
                throw new IllegalArgumentException("The application descriptor must be an instance of WebAppDescriptor");
            }
            return new MCRJerseyResourceTest.GrizzlyWebTestContainerApacheClient(super.create(baseUri, ad));
        }
    }
}
