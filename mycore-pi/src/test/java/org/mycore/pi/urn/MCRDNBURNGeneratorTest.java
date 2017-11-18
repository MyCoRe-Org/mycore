package org.mycore.pi.urn;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRDNBURNGeneratorTest extends MCRStoreTestCase {

    private static final String GENERATOR_ID = "TESTDNBURN1";

    private static final Logger LOGGER = LogManager.getLogger();

    @Rule
    public TemporaryFolder baseDir = new TemporaryFolder();

    @Test
    public void generate() throws Exception {
        MCRObjectID getID = MCRObjectID.getNextFreeId("test", "mock");
        MCRDNBURN generated = new MCRFLURNGenerator(GENERATOR_ID)
            .generate(getID, "");

        String urn = generated.asString();
        LOGGER.info("THE URN IS: {}", urn);

        Assert.assertFalse(urn.startsWith("urn:nbn:de:urn:nbn:de"));
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();

        testProperties.put("MCR.Access.Class", MCRAccessBaseImpl.class.getName());
        testProperties.put("MCR.Metadata.Type.mock", "true");
        testProperties.put("MCR.Metadata.Type.unregisterd", "true");

        testProperties.put("MCR.PI.Generator." + GENERATOR_ID, MCRFLURNGenerator.class.getName());
        testProperties.put("MCR.PI.Generator." + GENERATOR_ID + ".Namespace", "urn:nbn:de:gbv");

        return testProperties;
    }
}
