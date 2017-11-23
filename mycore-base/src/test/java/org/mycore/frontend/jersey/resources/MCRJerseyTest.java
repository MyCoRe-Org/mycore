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

package org.mycore.frontend.jersey.resources;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Application;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.mycore.common.MCRTestCaseHelper;
import org.mycore.frontend.jersey.MCRJerseyDefaultConfiguration;

/**
 * Jersey base test class. Overwrite this class and add a 
 * {@link BeforeClass} method which adds the required
 * jersey classes.
 * 
 * @author Matthias Eichner
 *
 */
public abstract class MCRJerseyTest extends JerseyTest {

    @ClassRule
    public static TemporaryFolder junitFolder = new TemporaryFolder();

    public static Set<Class<?>> JERSEY_CLASSES = new HashSet<>();

    @BeforeClass
    public static void mcrInitBaseDir() throws IOException {
        MCRTestCaseHelper.beforeClass(junitFolder);
        JERSEY_CLASSES.clear();
    }

    @AfterClass
    public static void mcrCleanUp() {
        JERSEY_CLASSES.clear();
    }

    @Before
    public void mcrSetUp() throws Exception {
        MCRTestCaseHelper.before(getTestProperties());
    }

    @After
    public void mcrTearDown() throws Exception {
        MCRTestCaseHelper.after();
    }

    protected Map<String, String> getTestProperties() {
        return new HashMap<>();
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(MultiPartFeature.class);
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExtendedGrizzlyTestContainerFactory();
    }

    @Override
    protected Application configure() {
        return new MCRJerseyTestResourceConfig();
    }

    protected static class MCRJerseyTestResourceConfig extends ResourceConfig {

        public MCRJerseyTestResourceConfig() {
            new MCRJerseyTestConfiguration().configure(this);
        }

    }

    protected static class MCRJerseyTestConfiguration extends MCRJerseyDefaultConfiguration {

        @Override
        protected void setupResources(ResourceConfig resourceConfig) {
            resourceConfig.registerClasses(JERSEY_CLASSES);
        }

        @Override
        protected void setupFeatures(ResourceConfig resourceConfig) {
            // no features
        }

    }

    private static class ExtendedGrizzlyTestContainerFactory implements TestContainerFactory {

        private static class GrizzlyTestContainer implements TestContainer {

            private final URI uri;

            private final ResourceConfig rc;

            private HttpServer server;

            private GrizzlyTestContainer(URI uri, ResourceConfig rc) {
                this.rc = rc;
                this.uri = uri;
            }

            @Override
            public ClientConfig getClientConfig() {
                return null;
            }

            @Override
            public URI getBaseUri() {
                return uri;
            }

            @Override
            public void start() {
                System.out.println("Starting GrizzlyTestContainer...");
                try {
                    this.server = GrizzlyHttpServerFactory.createHttpServer(uri, rc);

                    // Initialize and register Jersey Servlet
                    WebappContext context = new WebappContext("WebappContext", "");
                    ServletRegistration registration = context.addServlet("ServletContainer", ServletContainer.class);
                    registration.setInitParameter("javax.ws.rs.Application", rc.getClass().getName());
                    // Add an init parameter - this could be loaded from a parameter in the constructor
                    registration.setInitParameter("myparam", "myvalue");

                    registration.addMapping("/*");
                    context.deploy(server);
                } catch (ProcessingException e) {
                    throw new TestContainerException(e);
                }
            }

            @Override
            public void stop() {
                System.out.println("Stopping GrizzlyTestContainer...");
                this.server.shutdownNow();
            }
        }

        @Override
        public TestContainer create(URI baseUri, DeploymentContext deploymentContext) {
            return new GrizzlyTestContainer(baseUri, deploymentContext.getResourceConfig());
        }

    }

}
