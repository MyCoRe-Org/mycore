/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRDNBURNGeneratorTest extends MCRStoreTestCase {

    private static final String GENERATOR_ID = "TESTDNBURN1";

    private static final Logger LOGGER = LogManager.getLogger();

    @Rule
    public TemporaryFolder baseDir = new TemporaryFolder();

    @Test
    public void generate() throws Exception {
        MCRObjectID getID = MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId("test", "mock");
        MCRObject mcrObject1 = new MCRObject();
        mcrObject1.setId(getID);
        MCRFLURNGenerator flGenerator = MCRConfiguration2.getInstanceOfOrThrow(
            MCRFLURNGenerator.class, "MCR.PI.Generator." + GENERATOR_ID);
        MCRDNBURN generated = flGenerator.generate(mcrObject1, "");

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
