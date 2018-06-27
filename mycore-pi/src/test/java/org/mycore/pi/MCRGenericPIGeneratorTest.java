package org.mycore.pi;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRGenericPIGeneratorTest extends MCRStoreTestCase {

    @Test
    public void testGenerate() throws MCRPersistentIdentifierException {
        final MCRGenericPIGenerator generator = new MCRGenericPIGenerator("test1",
            "urn:nbn:de:gbv:$CurrentDate-$ObjectType-$ObjectProject-$ObjectNumber-$Count-", new SimpleDateFormat("yyyy", Locale.ROOT), null, null, 3,
            "dnbUrn");

        MCRObjectID testID = MCRObjectID.getInstance("my_test_00000001");
        MCRObject mcrObject = new MCRObject();
        mcrObject.setId(testID);

        final MCRPersistentIdentifier generate = generator.generate(mcrObject, "");
        final MCRPersistentIdentifier generate2 = generator.generate(mcrObject, "");
        assertEquals("urn:nbn:de:gbv:" + Calendar.getInstance().get(Calendar.YEAR) + "-test-my-00000001-000-5",
            generate.asString());
        assertEquals("urn:nbn:de:gbv:" + Calendar.getInstance().get(Calendar.YEAR) + "-test-my-00000001-001-2",
            generate2.asString());
    }

    @Override
    protected Map<String, String> getTestProperties() {
        final Map<String, String> testProperties = super.getTestProperties();

        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());

        return testProperties;
    }
}
