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

import static org.junit.Assert.assertTrue;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRRateLimitBuckets;
import org.mycore.common.MCRTestCase;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

import io.github.bucket4j.Bucket;

public class MCRRateLimitResolverTest extends MCRTestCase {

    final JDOMSource resultSource = new JDOMSource(new Document(new Element("result")));

    public static final String RATE_LIMIT_CALL = "ratelimit:Test:Mock:nothing";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MCRConfiguration2.set("MCR.URIResolver.ModuleResolver.ratelimit", "org.mycore.common.xml.MCRRateLimitResolver");
        MCRMockResolver.setResultSource(resultSource);
        MCRRateLimitBuckets.clearAllBuckets();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        MCRMockResolver.clearCalls();
    }

    /**
     * Tests, if the rate-limiting throws an exception upon consuming all tokens.
     */
    @Test
    public void testResolveError() {
        MCRConfiguration2.set("MCR.RateLimitResolver.Test.Behavior", "error");
        MCRConfiguration2.set("MCR.RateLimitResolver.Test.Limits", "100/D, 12/h, 6/min");
        final Bucket bucket = MCRRateLimitBuckets.getOrCreateBucket("Test");
        bucket.tryConsumeAsMuchAsPossible();
        Assert.assertThrows(MCRException.class, () -> MCRURIResolver.obtainInstance().resolve(RATE_LIMIT_CALL, null));
    }

    /**
     * Tests, if the blocking of downstream processing by the RateLimitResolver works properly.
     */
    @Test
    public void testResolveBlocking() throws TransformerException {
        MCRConfiguration2.set("MCR.RateLimitResolver.Test.Behavior", "block");
        MCRConfiguration2.set("MCR.RateLimitResolver.Test.Limits", "10/s");
        final Bucket bucket = MCRRateLimitBuckets.getOrCreateBucket("Test");
        bucket.tryConsumeAsMuchAsPossible();
        MCRURIResolver.obtainInstance().resolve(RATE_LIMIT_CALL, null);
    }

    /**
     * Tests, if the RateLimitResolver returns an empty Source-object if the configured behavior upon
     * reaching the rate limit is "empty".
     */
    @Test
    public void testResolveEmpty() throws TransformerException {
        MCRConfiguration2.set("MCR.RateLimitResolver.Test.Behavior", "empty");
        MCRConfiguration2.set("MCR.RateLimitResolver.Test.Limits", "10/min");
        final Bucket bucket = MCRRateLimitBuckets.getOrCreateBucket("Test");
        bucket.tryConsumeAsMuchAsPossible();
        Source emptySource = MCRURIResolver.obtainInstance().resolve(RATE_LIMIT_CALL, null);
        Assert.assertNotNull(emptySource);
        Assert.assertFalse(emptySource.isEmpty());
        Assert.assertEquals("", emptySource.getSystemId());
    }

    /**
     * Tests the behavior of the {@link MCRRateLimitResolver} and {@link MCRRateLimitBuckets} in case
     * of missing or wrong config.
     */
    @Test
    public void testResolveMalformedConfig() {
        // Test wrong value for behavior
        MCRConfiguration2.set("MCR.RateLimitResolver.Test.Behavior", "blocking");
        MCRConfiguration2.set("MCR.RateLimitResolver.Test.Limits", "10/s");

        MCRConfigurationException mcrConfigurationException = Assert.assertThrows(MCRConfigurationException.class,
            () -> MCRURIResolver.obtainInstance().resolve(RATE_LIMIT_CALL, null));

        assertTrue(mcrConfigurationException.getMessage()
            .contains("The behavior for ID Test is not correctly configured"));
        assertTrue(mcrConfigurationException.getCause().getMessage()
            .contains("not a valid type of enum RateLimitBehavior: blocking"));

        // Test wrong value for time unit
        MCRConfiguration2.set("MCR.RateLimitResolver.Test1.Behavior", "block");
        MCRConfiguration2.set("MCR.RateLimitResolver.Test1.Limits", "10/second");
        MCRConfigurationException mcrConfigurationException1 = Assert.assertThrows(MCRConfigurationException.class,
            () -> MCRRateLimitBuckets.getOrCreateBucket("Test1"));
        assertTrue(mcrConfigurationException1.getMessage().contains("10 tokens per second"));
        assertTrue(mcrConfigurationException1.getMessage().contains("Test1"));

        // Test negative value for token amount
        MCRConfiguration2.set("MCR.RateLimitResolver.Test2.Behavior", "block");
        MCRConfiguration2.set("MCR.RateLimitResolver.Test2.Limits", "-10/s");
        IllegalArgumentException illegalArgumentException =
            Assert.assertThrows(IllegalArgumentException.class, () -> MCRRateLimitBuckets.getOrCreateBucket("Test2"));
        assertTrue(illegalArgumentException.getMessage().contains("-10"));
        assertTrue(illegalArgumentException.getMessage().contains("capacity should be positive"));

        // Test non-number value for token amount
        MCRConfiguration2.set("MCR.RateLimitResolver.Test3.Behavior", "block");
        MCRConfiguration2.set("MCR.RateLimitResolver.Test3.Limits", "abc/s");
        NumberFormatException numberFormatException =
            Assert.assertThrows(NumberFormatException.class, () -> MCRRateLimitBuckets.getOrCreateBucket("Test3"));
        assertTrue(numberFormatException.getMessage().contains("abc"));

        // Test missing config
        MCRConfigurationException mcrConfigurationException2 = Assert.assertThrows(MCRConfigurationException.class,
            () -> MCRRateLimitBuckets.getOrCreateBucket("Test4"));
        assertTrue(mcrConfigurationException2.getMessage()
            .contains("Configuration property MCR.RateLimitResolver.Test4."));
    }

}
