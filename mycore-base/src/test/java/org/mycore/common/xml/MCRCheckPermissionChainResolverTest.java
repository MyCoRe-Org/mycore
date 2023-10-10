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

package org.mycore.common.xml;

import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.MCRAccessMock;
import org.mycore.common.MCRTestCase;

public class MCRCheckPermissionChainResolverTest extends MCRTestCase {

    private static final String RESOLVER_PREFIX = "checkPermissionChain";

    public static final String MOCK_RESOLVER_PREFIX = "Mock";

    private static final String MOCK_ID = "mcr_test_00000001";

    public static final String PERMISSION_USE_STUFF = "use-stuff";

    public static final String MOCK_CALL = MOCK_RESOLVER_PREFIX + ":nothing";

    public static final String READ_CALL = RESOLVER_PREFIX + ":" + MOCK_ID + ":" + MCRAccessManager.PERMISSION_READ
        + ":" + MOCK_CALL;

    public static final String USE_STUFF_CALL = RESOLVER_PREFIX + "::" + PERMISSION_USE_STUFF + ":" + MOCK_CALL;

    final JDOMSource resultSource = new JDOMSource(new Document(new Element("result")));

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MCRMockResolver.setResultSource(resultSource);
    }

    @Test
    public void resolveReadObjectForbidden() {
        MCRAccessMock.setMethodResult(false);

        Assert.assertThrows(TransformerException.class, () -> MCRURIResolver.instance().resolve(READ_CALL, null));
        assertReadCall();
        Assert.assertEquals("The resolver should not have been called", 0, MCRMockResolver.getCalls().size());
    }

    @Test
    public void resolveReadObjectAllowed() {
        MCRAccessMock.setMethodResult(true);
        Source result = null;

        try {
            result = MCRURIResolver.instance().resolve(READ_CALL, null);
        } catch (TransformerException e) {
            Assert.fail("Exception thrown!");
        }
        assertReadCall();
        Assert.assertEquals("The result source should be returned", resultSource, result);
        Assert.assertEquals("The resolver should have been called", 1, MCRMockResolver.getCalls().size());
        Assert.assertEquals("The Mock resolver should have been called with the right uri", MOCK_CALL,
            MCRMockResolver.getCalls().get(0).getHref());
    }

    @Test
    public void resolvePermissionAllowed() {
        MCRAccessMock.setMethodResult(true);
        Source result = null;

        try {
            result = MCRURIResolver.instance().resolve(USE_STUFF_CALL, null);
        } catch (TransformerException e) {
            Assert.fail("Exception thrown!");
        }
        assertPermissionCall();
        Assert.assertEquals("The result source should be returned", resultSource, result);
        Assert.assertEquals("The resolver should have been called", 1, MCRMockResolver.getCalls().size());
        Assert.assertEquals("The Mock resolver should have been called with the right uri", MOCK_CALL,
            MCRMockResolver.getCalls().get(0).getHref());

    }

    @Test
    public void resolvePermissionForbidden() {
        MCRAccessMock.setMethodResult(false);

        Assert.assertThrows(TransformerException.class, () -> MCRURIResolver.instance().resolve(USE_STUFF_CALL, null));
        assertPermissionCall();
        Assert.assertEquals("The resolver should not have been called", 0, MCRMockResolver.getCalls().size());
    }

    private void assertPermissionCall() {
        Assert.assertEquals("There should be a call to the access strategy", 1,
            MCRAccessMock.getCheckPermissionCalls().size());
        Assert.assertEquals("The call should be made with permission " + PERMISSION_USE_STUFF, PERMISSION_USE_STUFF,
            MCRAccessMock.getCheckPermissionCalls().get(0).getPermission());
        Assert.assertNull("The call should be made with null as id ",
            MCRAccessMock.getCheckPermissionCalls().get(0).getId());
    }

    private void assertReadCall() {
        Assert.assertEquals("There should be a call to the access strategy", 1,
            MCRAccessMock.getCheckPermissionCalls().size());
        Assert.assertEquals("The call should be made with permission read", MCRAccessManager.PERMISSION_READ,
            MCRAccessMock.getCheckPermissionCalls().get(0).getPermission());
        Assert.assertEquals("The call should be made with the id " + MOCK_ID, MOCK_ID,
            MCRAccessMock.getCheckPermissionCalls().get(0).getId());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        MCRAccessMock.clearCheckPermissionCallsList();
        MCRMockResolver.clearCalls();
    }

    @Override
    protected Map<String, String> getTestProperties() {
        final Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Access.Class", MCRAccessMock.class.getName());
        testProperties.put("MCR.URIResolver.ModuleResolver." + MOCK_RESOLVER_PREFIX, MCRMockResolver.class.getName());
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());

        return testProperties;
    }
}
