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

package org.mycore.mcr.acl.accesskey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mycore.access.MCRAccessManager.PERMISSION_READ;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

public class MCRAccessKeyTransformerTest extends MCRTestCase {

    private static final String OBJECT_ID = "mcr_test_00000001";

    private static final String READ_KEY = "blah";

    private static final String WRITE_KEY = "blub";

    private static MCRObjectID objectId;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectId = MCRObjectID.getInstance(OBJECT_ID);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        return testProperties;
    }

    @Test
    public void testAccessKeysTransform() {
        final MCRAccessKey accessKey = new MCRAccessKey(READ_KEY, PERMISSION_READ);
        final List<MCRAccessKey> accessKeys = new ArrayList<>();
        accessKeys.add(accessKey);
        final String json = MCRAccessKeyTransformer.jsonFromAccessKeys(accessKeys);
        final List<MCRAccessKey> transAccessKeys = MCRAccessKeyTransformer.accessKeysFromJson(json);
        final MCRAccessKey transAccessKey = transAccessKeys.get(0);
        assertNull(transAccessKey.getObjectId());
        assertEquals(transAccessKey.getId(), 0);
        assertEquals(accessKey.getSecret(), transAccessKey.getSecret());
        assertEquals(accessKey.getType(), transAccessKey.getType());
    }

    @Test
    public void testElementTransform() throws IOException {
        final MCRAccessKey accessKeyRead = new MCRAccessKey(READ_KEY, PERMISSION_READ);
        final MCRAccessKey accessKeyWrite = new MCRAccessKey(WRITE_KEY, PERMISSION_WRITE);
        final List<MCRAccessKey> accessKeys = new ArrayList<>();
        accessKeys.add(accessKeyRead);
        accessKeys.add(accessKeyWrite);
        final Element element = MCRAccessKeyTransformer.elementFromAccessKeys(accessKeys);
        new XMLOutputter(Format.getPrettyFormat()).output(element, System.out);
        final List<MCRAccessKey> transAccessKeys = MCRAccessKeyTransformer.accessKeysFromElement(objectId, element);
        assertEquals(2, transAccessKeys.size());
        final MCRAccessKey transAccessKeyRead = transAccessKeys.get(0);
        assertEquals(accessKeyRead.getSecret(), transAccessKeyRead.getSecret());
        assertEquals(accessKeyRead.getType(), transAccessKeyRead.getType());
    }

    @Test
    public void testServiceTransform() throws IOException {
        final Element service = new Element("service");
        final Element servFlags = new Element("servflags");
        final MCRAccessKey accessKey = new MCRAccessKey(READ_KEY, PERMISSION_READ);
        final List<MCRAccessKey> accessKeys = new ArrayList<>();
        accessKeys.add(accessKey);
        final String accessKeysJson = MCRAccessKeyTransformer.jsonFromAccessKeys(accessKeys);
        final Element servFlag = createServFlag("accesskeys", accessKeysJson);
        servFlags.addContent(servFlag);
        final Element sf1 = createServFlag("createdby", "administrator");
        sf1.setText("administrator");
        servFlags.addContent(sf1);
        service.addContent(servFlags);
        new XMLOutputter(Format.getPrettyFormat()).output(service, System.out);
        final List<MCRAccessKey> transAccessKeys = MCRAccessKeyTransformer.accessKeysFromElement(objectId, service);
        final MCRAccessKey transAccessKey = transAccessKeys.get(0);
        assertEquals(accessKey.getSecret(), transAccessKey.getSecret());
        assertEquals(accessKey.getType(), transAccessKey.getType());
    }

    private static Element createServFlag(final String type, final String content) {
        final Element servFlag = new Element("servflag");
        servFlag.setAttribute("type", type);
        servFlag.setAttribute("inherited", "0");
        servFlag.setAttribute("form", "plain");
        servFlag.setText(content);
        return servFlag;
    }
}
