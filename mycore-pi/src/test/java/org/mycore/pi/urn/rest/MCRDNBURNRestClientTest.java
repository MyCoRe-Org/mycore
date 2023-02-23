package org.mycore.pi.urn.rest;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.MCRPITestUtils;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRDNBURNRestClientTest extends MCRStoreTestCase{
    @Ignore
    @Test
    public void testRegister() throws MalformedURLException, MCRPersistentIdentifierException {
        MCRDNBURNRestClient mcrdnburnRestClient = new MCRDNBURNRestClient(MCRDNBURNRestClientTest::getUrlOfUrn);
        MCRPIRegistrationInfo urn = MCRPITestUtils.generateMCRPI("test.jpg", "TestService", "namespace-");
        mcrdnburnRestClient.register(urn);
        mcrdnburnRestClient.register(urn);
    }

    private static URL getUrlOfUrn(MCRPIRegistrationInfo urn) {
        String fileName = UUID.randomUUID().toString() + ".jpg";
        try {
            return URI.create("http://foo.com/").resolve(fileName).toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();;
        }

        return null;
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();

        testProperties.put("MCR.PI.DNB.Credentials.Login", "username");
        testProperties.put("MCR.PI.DNB.Credentials.Password", "password");
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        testProperties.put("MCR.PI.URNGranular.API.BaseURL", "https://api.nbn-resolving.org/sandbox/v2/");
        return testProperties;
    }
}
