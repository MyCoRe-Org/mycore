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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
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

import jakarta.ws.rs.core.Application;

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
    protected Application configure() {
        return new MCRJerseyTestResourceConfig();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new GrizzlyWebTestContainerFactory();
    }

    @Override
    protected DeploymentContext configureDeployment() {
        return ServletDeploymentContext.forServlet(new ServletContainer((ResourceConfig) configure())).build();
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

}
