package org.mycore.frontend.jersey.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationLoader;
import org.mycore.common.config.MCRConfigurationLoaderFactory;
import org.mycore.frontend.jersey.MCRJerseyResourceTest;
import org.mycore.frontend.jersey.filter.MCRSecurityFilterFactory;
import org.mycore.frontend.jersey.resources.MCRTestResource;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.spi.container.ResourceFilter;

public class MCRResourceSecurityTest extends MCRJerseyResourceTest {
//    public static class MyAccessManagerConnector extends MCRAccessManagerConnector {
//        private HashMap<String, Boolean> permissions = new HashMap<String, Boolean>();
//
//        public MyAccessManagerConnector() {
//            permissions.put(decodeRule(MCRTestResource.class.getName(), "/auth_GET"), true);
//            permissions.put(decodeRule(MCRTestResource.class.getName(), "/auth/logout/{id}_GET"), false);
//        }
//
//        private String decodeRule(String id, String permission) {
//            return id + "::" + permission;
//        }
//
//        @Override
//        public boolean checkPermission(String id, String permission, MCRSession session) {
//            Boolean perm = permissions.get(decodeRule(id, permission));
//            if (perm == null) {
//                throw new RuntimeException("could not find permisson for: " + id + " # " + permission);
//            }
//
//            return perm;
//        }
//    }

    @BeforeClass
    public static void init() {
        MCRConfiguration mcrProperties = MCRConfiguration.instance();
        MCRConfigurationLoader configLoader = MCRConfigurationLoaderFactory.getConfigurationLoader();
        mcrProperties.initialize(configLoader.load(), true);
//        mcrProperties.set("McrSessionSecurityFilter.MCRAccessManager.Connector", MyAccessManagerConnector.class.getName());
        mcrProperties.set("MCR.Persistence.Database.Enable", "false");
    }

    @Test
    @Ignore
    public void testResourceSecurity() throws Exception {
        ClientResponse response = resource().path("/auth").get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response = resource().path("/auth/logout/foo").get(ClientResponse.class);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        Map<String, List<String>> resourceRegister = MCRResourceSercurityConf.instance().getResourceRegister();
        //        assertEquals(1, resourceRegister.size());
        List<String> testResourceEntry = MCRResourceSercurityConf.instance().getResourceRegister().get(MCRTestResource.class.getName());
        assertNotNull(MCRTestResource.class.getName() + " should has been registered", testResourceEntry);
        //        assertEquals(2, testResourceEntry.size());
    }

    @Override
    public String[] getPackageName() {
        return new String[] { MCRTestResource.class.getPackage().getName() };
    }

    @Override
    public Map<String, String> getInitParams() {
        Map<String, String> initParams = new HashMap<String, String>();
        initParams.put(ResourceFilter.class.getName() + "s", MCRSecurityFilterFactory.class.getName());
        return initParams;
    }

}
