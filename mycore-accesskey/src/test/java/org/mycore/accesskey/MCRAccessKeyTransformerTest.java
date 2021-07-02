/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.accesskey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import org.junit.Test;

import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.accesskey.backend.MCRAccessKey;
import org.mycore.common.MCRTestCase;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;

import org.xml.sax.SAXParseException;

public class MCRAccessKeyTransformerTest extends MCRTestCase {

    private static final String MCR_OBJECT_ID = "mcr_test_00000001";

    private static final String READ_KEY = "blah";

    private static final String WRITE_KEY = "blub";

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        return testProperties;
    }

    public void testAccessKeysTransform() throws IOException {
        final MCRAccessKey accessKey = new MCRAccessKey(MCRObjectID.getInstance(MCR_OBJECT_ID), READ_KEY, MCRAccessManager.PERMISSION_READ);
        final List<MCRAccessKey> accessKeys = new ArrayList<MCRAccessKey>();
        accessKeys.add(accessKey);
        final String json = MCRAccessKeyTransformer.jsonFromAccessKeys(accessKeys);
        final List<MCRAccessKey> transAccessKeys = MCRAccessKeyTransformer.accessKeysFromJson(json);
        final MCRAccessKey transAccessKey = transAccessKeys.get(0);
        assertEquals(transAccessKey.getObjectId(), null);
        assertEquals(transAccessKey.getId(), 0);
        assertEquals(accessKey.getValue(), transAccessKey.getValue());
        assertEquals(accessKey.getType(), transAccessKey.getType());
    }

    @Test
    public void testServFlagTransform() throws IOException {
        final MCRAccessKey accessKeyRead = new MCRAccessKey(MCRObjectID.getInstance(MCR_OBJECT_ID), READ_KEY, MCRAccessManager.PERMISSION_READ);
        final MCRAccessKey accessKeyWrite = new MCRAccessKey(MCRObjectID.getInstance(MCR_OBJECT_ID), WRITE_KEY, MCRAccessManager.PERMISSION_WRITE);
        final List<MCRAccessKey> accessKeys = new ArrayList<MCRAccessKey>();
        accessKeys.add(accessKeyRead);
        accessKeys.add(accessKeyWrite);
        final Element servFlag = MCRAccessKeyTransformer.servFlagFromAccessKeys(accessKeys);
        new XMLOutputter(Format.getPrettyFormat()).output(servFlag, System.out);
        final List<MCRAccessKey> transAccessKeys = MCRAccessKeyTransformer.accessKeysFromElement(
            MCRObjectID.getInstance(MCR_OBJECT_ID), servFlag);
        assertTrue(transAccessKeys.size() == 2);
        final MCRAccessKey transAccessKeyRead = transAccessKeys.get(0);
        assertEquals(accessKeyRead.getObjectId(), transAccessKeyRead.getObjectId());
        assertEquals(accessKeyRead.getValue(), transAccessKeyRead.getValue());
        assertEquals(accessKeyRead.getType(), transAccessKeyRead.getType());
    }

    @Test
    public void testServiceTransform() throws IOException {
        final Element service = new Element("service");
        final Element servFlags = new Element("servflags");
        final MCRAccessKey accessKey = new MCRAccessKey(MCRObjectID.getInstance(MCR_OBJECT_ID), READ_KEY, MCRAccessManager.PERMISSION_READ);
        final List<MCRAccessKey> accessKeys = new ArrayList<MCRAccessKey>();
        accessKeys.add(accessKey);
        final Element servFlag = MCRAccessKeyTransformer.servFlagFromAccessKeys(accessKeys);
        servFlags.addContent(servFlag);
        final Element sf1 = new Element("servflag");
        sf1.setAttribute("type", "createdby");
        sf1.setAttribute("inherited", "0");
        sf1.setAttribute("form", "plain");
        sf1.setText("administrator");
        servFlags.addContent(sf1);
        service.addContent(servFlags);
        new XMLOutputter(Format.getPrettyFormat()).output(service, System.out);
        final List<MCRAccessKey> transAccessKeys = MCRAccessKeyTransformer.accessKeysFromElement(
            MCRObjectID.getInstance(MCR_OBJECT_ID), service);
        final MCRAccessKey transAccessKey = transAccessKeys.get(0);
        assertEquals(accessKey.getObjectId(), transAccessKey.getObjectId());
        assertEquals(accessKey.getValue(), transAccessKey.getValue());
        assertEquals(accessKey.getType(), transAccessKey.getType());
    }
}
