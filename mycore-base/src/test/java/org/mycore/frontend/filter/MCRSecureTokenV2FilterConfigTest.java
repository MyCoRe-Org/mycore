/**
 * 
 */
package org.mycore.frontend.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSecureTokenV2FilterConfigTest extends MCRTestCase {

    /**
     * Test method for {@link org.mycore.frontend.filter.MCRSecureTokenV2FilterConfig#getExtensionPattern(java.util.Collection)}.
     */
    @Test
    public void testGetExtensionPattern() {
        testMP4Files(MCRSecureTokenV2FilterConfig.getExtensionPattern(Arrays.asList("mp4")));
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
            assertTrue("Filename " + filename + " should match " + p, p.matcher(filename).matches());
        } else {
            assertFalse("Filename " + filename + " should not match " + p, p.matcher(filename).matches());
        }
    }

}
