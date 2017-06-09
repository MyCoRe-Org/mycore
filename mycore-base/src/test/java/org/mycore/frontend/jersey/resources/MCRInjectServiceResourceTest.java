package org.mycore.frontend.jersey.resources;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mycore.common.config.MCRConfiguration;

import com.google.inject.AbstractModule;

public class MCRInjectServiceResourceTest extends MCRJerseyTest {

    @Path("service")
    public static class ServiceTestCase {

        @Inject
        private TestService service;

        @GET
        public String get() {
            return service.get();
        }

    }

    @BeforeClass
    public static void register() {
        JERSEY_CLASSES.add(ServiceTestCase.class);
    }

    @Override
    public Application configure() {
        MCRConfiguration.instance().set("MCR.inject.module.GuiceTest", TestModule.class.getName());
        return super.configure();
    }

    @Test
    public void test() {
        final String hello = target("service").request().get(String.class);
        assertEquals("welcome to my service", hello);
    }

    public static class TestModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(TestService.class).to(TestServiceImpl.class);
        }

    }

    private interface TestService {

        String get();

    }

    private static class TestServiceImpl implements TestService {

        @Override
        public String get() {
            return "welcome to my service";
        }

    }

}
