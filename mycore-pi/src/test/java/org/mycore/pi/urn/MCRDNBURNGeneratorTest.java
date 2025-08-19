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

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@ExtendWith(MCRMetadataExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Access.Class", classNameOf = MCRAccessBaseImpl.class),
    @MCRTestProperty(key = "MCR.Metadata.Type.mock", string = "true"),
    @MCRTestProperty(key = "MCR.Metadata.Type.unregisterd", string = "true"),
    @MCRTestProperty(key = "MCR.PI.Generator." + MCRDNBURNGeneratorTest.GENERATOR_ID,
        classNameOf = MCRFLURNGenerator.class),
    @MCRTestProperty(key = "MCR.PI.Generator." + MCRDNBURNGeneratorTest.GENERATOR_ID + ".Namespace",
        string = "urn:nbn:de:gbv")
})
public class MCRDNBURNGeneratorTest {

    public static final String GENERATOR_ID = "TESTDNBURN1";

    private static final Logger LOGGER = LogManager.getLogger();

    @Test
    public void generate()  {
        MCRObjectID getID = MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId("test", "mock");
        MCRObject mcrObject1 = new MCRObject();
        mcrObject1.setId(getID);
        MCRFLURNGenerator flGenerator = MCRConfiguration2.getInstanceOfOrThrow(
            MCRFLURNGenerator.class, "MCR.PI.Generator." + GENERATOR_ID);
        MCRDNBURN generated = flGenerator.generate(mcrObject1, "");

        String urn = generated.asString();
        LOGGER.info("THE URN IS: {}", urn);

        assertFalse(urn.startsWith("urn:nbn:de:urn:nbn:de"));
    }

}
