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
import org.mycore.frontend.jersey.MCRJerseyDefaultConfiguration;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;

/**
 * Jersey test feature.
 *
 * @author Matthias Eichner
 */
public class MCRJerseyTestFeature {

    private MCRJerseyTest jerseyTest;

    public void setUp(Set<Class<?>> jerseyComponents) throws Exception {
        MCRJerseyTest.COMPONENTS.set(jerseyComponents);
        this.jerseyTest = new MCRJerseyTest();
        this.jerseyTest.setUp();
    }

    public void tearDown() throws Exception {
        this.jerseyTest.tearDown();
    }

    public WebTarget target(final String path) {
        return this.jerseyTest.target().path(path);
    }

    public MCRJerseyTest test() {
        return jerseyTest;
    }

    public static class MCRJerseyTest extends JerseyTest {

        private static final ThreadLocal<Set<Class<?>>> COMPONENTS = new ThreadLocal<>();

        @Override
        protected Application configure() {
            return new MCRJerseyTestResourceConfig(COMPONENTS.get());
        }

        @Override
        protected void configureClient(ClientConfig config) {
            config.register(MultiPartFeature.class);
        }

        @Override
        protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
            return new GrizzlyWebTestContainerFactory();
        }

        @Override
        protected DeploymentContext configureDeployment() {
            return ServletDeploymentContext.forServlet(new ServletContainer((ResourceConfig) configure())).build();
        }

    }

    protected static class MCRJerseyTestResourceConfig extends ResourceConfig {

        public MCRJerseyTestResourceConfig(Set<Class<?>> components) {
            new MCRJerseyTestConfiguration(components).configure(this);
        }

    }

    protected static class MCRJerseyTestConfiguration extends MCRJerseyDefaultConfiguration {

        private final Set<Class<?>> components;

        public MCRJerseyTestConfiguration(Set<Class<?>> components) {
            this.components = components;
        }

        @Override
        protected void setupResources(ResourceConfig resourceConfig) {
            resourceConfig.registerClasses(this.components);
        }

        @Override
        protected void setupFeatures(ResourceConfig resourceConfig) {
            // no features
        }

    }

}
