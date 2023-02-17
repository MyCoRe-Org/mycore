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
 
package org.mycore.orcid2.metadata;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.orcid2.exception.MCRORCIDTransformationException;

public class MCRORCIDMetadataUtilsTest extends MCRTestCase {

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.ORCID2.Metadata.SaveOtherPutCodes", Boolean.TRUE.toString());
        return testProperties;
    }

    @Test
    public void testGetORCIDFlagContentNull() {
        assertNull(MCRORCIDMetadataUtils.getORCIDFlagContent(new MCRObject()));
    }

    @Test(expected = MCRORCIDTransformationException.class)
    public void testTransformFlagContentStringException() {
        MCRORCIDMetadataUtils.transformFlagContentString("invalid json");
    }

    @Test
    public void testTransformFlagContent() throws MCRORCIDTransformationException {
        final MCRORCIDFlagContent flagContent = new MCRORCIDFlagContent();
        String result = MCRORCIDMetadataUtils.transformFlagContent(flagContent);
        String expectedResult = "{\"userInfos\":[]}";
        assertEquals(result, expectedResult);

        final MCRORCIDUserInfo userInfo = new MCRORCIDUserInfo("ORCID");
        flagContent.getUserInfos().add(userInfo);
        result = MCRORCIDMetadataUtils.transformFlagContent(flagContent);
        expectedResult = "{\"userInfos\":[{\"orcid\":\"ORCID\"}]}";
        assertEquals(result, expectedResult);

        final MCRORCIDPutCodeInfo putCodeInfo = new MCRORCIDPutCodeInfo();
        userInfo.setWorkInfo(putCodeInfo);
        result = MCRORCIDMetadataUtils.transformFlagContent(flagContent);
        expectedResult = "{\"userInfos\":[{\"orcid\":\"ORCID\",\"works\":{}}]}";
        assertEquals(result, expectedResult);

        putCodeInfo.setOwnPutCode(1);
        result = MCRORCIDMetadataUtils.transformFlagContent(flagContent);
        expectedResult = "{\"userInfos\":[{\"orcid\":\"ORCID\",\"works\":{\"own\":1}}]}";
        assertEquals(result, expectedResult);
    }

    @Test
    public void testDoSetORCIDFlagContent() throws MCRORCIDTransformationException {
        final MCRObject object = new MCRObject();
        final MCRORCIDFlagContent flagContent = new MCRORCIDFlagContent();
        MCRORCIDMetadataUtils.doSetORCIDFlagContent(object, flagContent);
        List<String> flags = object.getService().getFlags("MyCoRe-ORCID");
        assertEquals(flags.size(), 1);

        final MCRORCIDUserInfo userInfo = new MCRORCIDUserInfo("ORCID");
        flagContent.getUserInfos().add(userInfo);
        MCRORCIDMetadataUtils.doSetORCIDFlagContent(object, flagContent);
        flags = object.getService().getFlags("MyCoRe-ORCID");
        assertEquals(flags.size(), 1);
        final MCRORCIDFlagContent result = MCRORCIDMetadataUtils.transformFlagContentString(flags.get(0));
        assertEquals(flagContent, result);
    }

    @Test
    public void testDoRemoveORCIDFlag() {
        final MCRObject object = new MCRObject();
        final MCRORCIDFlagContent flagContent = new MCRORCIDFlagContent();
        MCRORCIDMetadataUtils.doSetORCIDFlagContent(object, flagContent);
        MCRORCIDMetadataUtils.doRemoveORCIDFlag(object);
        assertEquals(object.getService().getFlags("MyCoRe-ORCID").size(), 0);
    }
}
