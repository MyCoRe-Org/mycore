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

package org.mycore.frontend;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;

@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.baseurl", string = MCRFrontendUtilTest.BASE_URL)
})
public class MCRFrontendUtilTest extends MCRTestCase {

    static final String BASE_URL = "http://localhost:8080/app/";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        MCRFrontendUtil.prepareBaseURLs(BASE_URL);
    }

    @Test
    public void testSafeRedirects() {
        assertTrue(MCRFrontendUtil.isSafeRedirect("/foo"));
        assertTrue(MCRFrontendUtil.isSafeRedirect("/receive/junit_test_00000001?XSL.a=b"));
        assertTrue(MCRFrontendUtil.isSafeRedirect(BASE_URL));
        assertTrue(MCRFrontendUtil.isSafeRedirect(BASE_URL + "receive/junit_test_00000001"));
    }

    @Test
    public void testUnsafeRedirects() {
        assertFalse(MCRFrontendUtil.isSafeRedirect(null));
        assertFalse(MCRFrontendUtil.isSafeRedirect(""));
        assertFalse(MCRFrontendUtil.isSafeRedirect("foo"));
        assertFalse(MCRFrontendUtil.isSafeRedirect("//evil.com"));
        assertFalse(MCRFrontendUtil.isSafeRedirect("///evil.com"));
        assertFalse(MCRFrontendUtil.isSafeRedirect("////evil.com"));
        assertFalse(MCRFrontendUtil.isSafeRedirect("/\\evil.com"));
        assertFalse(MCRFrontendUtil.isSafeRedirect("/\t/evil.com"));
        assertFalse(MCRFrontendUtil.isSafeRedirect("/foo/../../evil"));
        assertFalse(MCRFrontendUtil.isSafeRedirect("javascript:alert(1)"));
        assertFalse(MCRFrontendUtil.isSafeRedirect("https://evil.com/"));
        assertFalse(MCRFrontendUtil.isSafeRedirect("http://localhost:8080.evil.com/app/"));
        assertFalse(MCRFrontendUtil.isSafeRedirect("http://localhost:8080/app@evil.com/"));
        assertFalse(MCRFrontendUtil.isSafeRedirect("http://localhost:9090/app/"));
        assertFalse(MCRFrontendUtil.isSafeRedirect("https://localhost:8080/app/"));
        assertFalse(MCRFrontendUtil.isSafeRedirect("http:///evil.com"));
    }
}
