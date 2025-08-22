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

package org.mycore.frontend.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.mycore.test.MyCoReTest;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@MyCoReTest
public class MCRSecureTokenV2FilterConfigTest {

    /**
     * Test method for {@link org.mycore.frontend.filter.MCRSecureTokenV2FilterConfig#getExtensionPattern(java.util.Collection)}.
     */
    @Test
    public void testGetExtensionPattern() {
        testMP4Files(MCRSecureTokenV2FilterConfig.getExtensionPattern(Collections.singletonList("mp4")));
        testMP4Files(MCRSecureTokenV2FilterConfig.getExtensionPattern(Arrays.asList("flv", "mp4")));
        testMP4Files(MCRSecureTokenV2FilterConfig.getExtensionPattern(Arrays.asList("mp4", "flv")));
    }

    private void testMP4Files(Pattern mp4) {
        testPattern(mp4, "test.mp4", true);
        testPattern(mp4, "test.mymp4", false);
        testPattern(mp4, "test.mp45", false);
        testPattern(mp4, "my test.mp4", true);
    }

    private void testPattern(Pattern p, String filename, boolean result) {
        if (result) {
            assertTrue(p.matcher(filename).matches(), "Filename " + filename + " should match " + p);
        } else {
            assertFalse(p.matcher(filename).matches(), "Filename " + filename + " should not match " + p);
        }
    }

}
