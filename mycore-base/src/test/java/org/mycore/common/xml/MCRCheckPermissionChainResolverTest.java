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

package org.mycore.common.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.MCRAccessMock;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Access.Class", classNameOf = MCRAccessMock.class),
    @MCRTestProperty(key = "MCR.URIResolver.ModuleResolver." + MCRCheckPermissionChainResolverTest.MOCK_RESOLVER_PREFIX,
        classNameOf = MCRMockResolver.class),
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
})
public class MCRCheckPermissionChainResolverTest {

    private static final String RESOLVER_PREFIX = "checkPermissionChain";

    public static final String MOCK_RESOLVER_PREFIX = "Mock";

    private static final String MOCK_ID = "mcr_test_00000001";

    public static final String PERMISSION_USE_STUFF = "use-stuff";

    public static final String MOCK_CALL = MOCK_RESOLVER_PREFIX + ":nothing";

    public static final String READ_CALL = RESOLVER_PREFIX + ":" + MOCK_ID + ":" + MCRAccessManager.PERMISSION_READ
        + ":" + MOCK_CALL;

    public static final String USE_STUFF_CALL = RESOLVER_PREFIX + "::" + PERMISSION_USE_STUFF + ":" + MOCK_CALL;

    final JDOMSource resultSource = new JDOMSource(new Document(new Element("result")));

    @BeforeEach
    public void setUp() throws Exception {
        MCRMockResolver.setResultSource(resultSource);
        MCRURIResolver.obtainInstance().reinitialize();
    }

    @Test
    public void resolveReadObjectForbidden() {
        MCRAccessMock.setMethodResult(false);

        assertThrows(TransformerException.class, () -> MCRURIResolver.obtainInstance().resolve(READ_CALL, null));
        assertReadCall();
        assertEquals(0, MCRMockResolver.getCalls().size(), "The resolver should not have been called");
    }

    @Test
    public void resolveReadObjectAllowed() {
        MCRAccessMock.setMethodResult(true);
        Source result = null;

        try {
            result = MCRURIResolver.obtainInstance().resolve(READ_CALL, null);
        } catch (TransformerException e) {
            fail("Exception thrown!");
        }
        assertReadCall();
        assertEquals(resultSource, result, "The result source should be returned");
        assertEquals(1, MCRMockResolver.getCalls().size(), "The resolver should have been called");
        assertEquals(MOCK_CALL, MCRMockResolver.getCalls().getFirst().getHref(),
            "The Mock resolver should have been called with the right uri");
    }

    @Test
    public void resolvePermissionAllowed() {
        MCRAccessMock.setMethodResult(true);
        Source result = null;

        try {
            result = MCRURIResolver.obtainInstance().resolve(USE_STUFF_CALL, null);
        } catch (TransformerException e) {
            fail("Exception thrown!");
        }
        assertPermissionCall();
        assertEquals(resultSource, result, "The result source should be returned");
        assertEquals(1, MCRMockResolver.getCalls().size(), "The resolver should have been called");
        assertEquals(MOCK_CALL, MCRMockResolver.getCalls().getFirst().getHref(),
            "The Mock resolver should have been called with the right uri");

    }

    @Test
    public void resolvePermissionForbidden() {
        MCRAccessMock.setMethodResult(false);

        assertThrows(TransformerException.class, () -> MCRURIResolver.obtainInstance()
            .resolve(USE_STUFF_CALL, null));
        assertPermissionCall();
        assertEquals(0, MCRMockResolver.getCalls().size(), "The resolver should not have been called");
    }

    private void assertPermissionCall() {
        assertEquals(1, MCRAccessMock.getCheckPermissionCalls().size(),
            "There should be a call to the access strategy");
        assertEquals(PERMISSION_USE_STUFF, MCRAccessMock.getCheckPermissionCalls().getFirst().getPermission(),
            "The call should be made with permission " + PERMISSION_USE_STUFF);
        assertNull(MCRAccessMock.getCheckPermissionCalls().getFirst().getId(),
            "The call should be made with null as id ");
    }

    private void assertReadCall() {
        assertEquals(1, MCRAccessMock.getCheckPermissionCalls().size(),
            "There should be a call to the access strategy");
        assertEquals(MCRAccessManager.PERMISSION_READ,
            MCRAccessMock.getCheckPermissionCalls().getFirst().getPermission(),
            "The call should be made with permission read");
        assertEquals(MOCK_ID, MCRAccessMock.getCheckPermissionCalls().getFirst().getId(),
            "The call should be made with the id " + MOCK_ID);
    }

    @AfterEach
    public void tearDown() throws Exception {
        MCRAccessMock.clearCheckPermissionCallsList();
        MCRMockResolver.clearCalls();
    }

}
