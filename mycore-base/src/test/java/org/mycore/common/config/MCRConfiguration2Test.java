package org.mycore.common.config;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import com.google.inject.AbstractModule;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRConfiguration2Test extends MCRTestCase {

    @Test
    public void instantiateClass() {
        TestClass testClass = MCRConfiguration2.instantiateClass(TestClass.class.getName());
        assertEquals("welcome to my service", testClass.useService());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MCRConfiguration2.set("MCR.Inject.Module.GuiceTest", TestModule.class.getName());
    }

    public static class TestModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(TestService.class).to(TestServiceImpl.class);
        }

    }

    private static class TestClass {

        @Inject
        private TestService service;

        public String useService() {
            return service.get();
        }

    }

    private interface TestService {

        String get();

    }

    private static class TestServiceImpl implements MCRConfiguration2Test.TestService {

        @Override
        public String get() {
            return "welcome to my service";
        }

    }

}
