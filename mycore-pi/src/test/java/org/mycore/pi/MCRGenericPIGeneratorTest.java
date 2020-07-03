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

package org.mycore.pi;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRGenericPIGeneratorTest extends MCRStoreTestCase {

    public static final int CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR);

    @Test
    public void testGenerate() throws MCRPersistentIdentifierException {
        final MCRGenericPIGenerator generator = new MCRGenericPIGenerator("test1",
            "urn:nbn:de:gbv:$CurrentDate-$1-$2-$ObjectType-$ObjectProject-$ObjectNumber-$Count-",
            new SimpleDateFormat("yyyy", Locale.ROOT), null, null, 3,
            "dnbUrn", "/mycoreobject/metadata/test1/test2/text()", "/mycoreobject/metadata/test1/test3/text()");

        MCRObjectID testID = MCRObjectID.getInstance("my_test_00000001");
        MCRObject mcrObject = new MCRObject();
        mcrObject.setSchema("test");
        mcrObject.setId(testID);
        final Element metadata = new Element("metadata");
        final Element testElement = new Element("test1");
        metadata.addContent(testElement);
        testElement.setAttribute("class", "MCRMetaXML");
        testElement.addContent(new Element("test2").setText("result1"));
        testElement.addContent(new Element("test3").setText("result2"));
        mcrObject.getMetadata().setFromDOM(metadata);

        final String pi1 = generator.generate(mcrObject, "").asString();
        final String pi2 = generator.generate(mcrObject, "").asString();
        assertEquals("urn:nbn:de:gbv:" + CURRENT_YEAR + "-result1-result2-test-my-00000001-000-",
            pi1.substring(0, pi1.length() - 1));
        assertEquals("urn:nbn:de:gbv:" + CURRENT_YEAR + "-result1-result2-test-my-00000001-001-",
            pi2.substring(0, pi2.length() - 1));
    }

    @Override
    protected Map<String, String> getTestProperties() {
        final Map<String, String> testProperties = super.getTestProperties();

        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());

        return testProperties;
    }
}
