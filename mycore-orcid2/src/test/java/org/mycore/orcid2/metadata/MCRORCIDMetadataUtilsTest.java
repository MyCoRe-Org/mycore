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

package org.mycore.orcid2.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.orcid2.exception.MCRORCIDTransformationException;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.ORCID2.Metadata.SaveOtherPutCodes", string = "true")
})
public class MCRORCIDMetadataUtilsTest {

    @Test
    public void testGetORCIDFlagContentNull() {
        assertNull(MCRORCIDMetadataUtils.getORCIDFlagContent(new MCRObject()));
    }

    @Test
    public void testTransformFlagContentStringException() {
        assertThrows(
            MCRORCIDTransformationException.class,
            () -> MCRORCIDMetadataUtils.transformFlagContentString("invalid json"));
    }

    @Test
    public void testTransformFlagContent() {
        final MCRORCIDFlagContent flagContent = new MCRORCIDFlagContent();
        String result = MCRORCIDMetadataUtils.transformFlagContent(flagContent);
        String expectedResult = "{\"userInfos\":[]}";
        assertEquals(expectedResult, result);

        final MCRORCIDUserInfo userInfo = new MCRORCIDUserInfo("ORCID");
        flagContent.getUserInfos().add(userInfo);
        result = MCRORCIDMetadataUtils.transformFlagContent(flagContent);
        expectedResult = "{\"userInfos\":[{\"orcid\":\"ORCID\"}]}";
        assertEquals(expectedResult, result);

        final MCRORCIDPutCodeInfo putCodeInfo = new MCRORCIDPutCodeInfo();
        userInfo.setWorkInfo(putCodeInfo);
        result = MCRORCIDMetadataUtils.transformFlagContent(flagContent);
        expectedResult = "{\"userInfos\":[{\"orcid\":\"ORCID\",\"works\":{}}]}";
        assertEquals(expectedResult, result);

        putCodeInfo.setOwnPutCode(1);
        result = MCRORCIDMetadataUtils.transformFlagContent(flagContent);
        expectedResult = "{\"userInfos\":[{\"orcid\":\"ORCID\",\"works\":{\"own\":1}}]}";
        assertEquals(expectedResult, result);
    }

    @Test
    public void testDoSetORCIDFlagContent() {
        final MCRObject object = new MCRObject();
        final MCRORCIDFlagContent flagContent = new MCRORCIDFlagContent();
        MCRORCIDMetadataUtils.doSetORCIDFlagContent(object, flagContent);
        List<String> flags = object.getService().getFlags("MyCoRe-ORCID");
        assertEquals(1, flags.size());

        final MCRORCIDUserInfo userInfo = new MCRORCIDUserInfo("ORCID");
        flagContent.getUserInfos().add(userInfo);
        MCRORCIDMetadataUtils.doSetORCIDFlagContent(object, flagContent);
        flags = object.getService().getFlags("MyCoRe-ORCID");
        assertEquals(1, flags.size());
        final MCRORCIDFlagContent result = MCRORCIDMetadataUtils.transformFlagContentString(flags.getFirst());
        assertEquals(flagContent, result);
    }

    @Test
    public void testDoRemoveORCIDFlag() {
        final MCRObject object = new MCRObject();
        final MCRORCIDFlagContent flagContent = new MCRORCIDFlagContent();
        MCRORCIDMetadataUtils.doSetORCIDFlagContent(object, flagContent);
        MCRORCIDMetadataUtils.removeORCIDFlag(object);
        assertEquals(0, object.getService().getFlags("MyCoRe-ORCID").size());
    }
}
