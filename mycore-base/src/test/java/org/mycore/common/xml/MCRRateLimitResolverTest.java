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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRRateLimitBuckets;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.test.MyCoReTest;

import io.github.bucket4j.Bucket;

@MyCoReTest
public class MCRRateLimitResolverTest {

    final JDOMSource resultSource = new JDOMSource(new Document(new Element("result")));

    public static final String RATE_LIMIT_CALL = "ratelimit:Test:Mock:nothing";

    @BeforeEach
    public void setUp() throws Exception {
        MCRConfiguration2.set("MCR.URIResolver.ModuleResolver.ratelimit", "org.mycore.common.xml.MCRRateLimitResolver");
        MCRMockResolver.setResultSource(resultSource);
        MCRRateLimitBuckets.clearAllBuckets();
    }

    @AfterEach
    public void tearDown() throws Exception {
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
        assertThrows(MCRException.class, () -> MCRURIResolver.obtainInstance().resolve(RATE_LIMIT_CALL, null));
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
        assertNotNull(emptySource);
        assertFalse(emptySource.isEmpty());
        assertEquals("", emptySource.getSystemId());
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

        MCRConfigurationException mcrConfigurationException = assertThrows(MCRConfigurationException.class,
            () -> MCRURIResolver.obtainInstance().resolve(RATE_LIMIT_CALL, null));

        assertTrue(mcrConfigurationException.getMessage()
            .contains("The behavior for ID Test is not correctly configured"));
        assertTrue(mcrConfigurationException.getCause().getMessage()
            .contains("not a valid type of enum RateLimitBehavior: blocking"));

        // Test wrong value for time unit
        MCRConfiguration2.set("MCR.RateLimitResolver.Test1.Behavior", "block");
        MCRConfiguration2.set("MCR.RateLimitResolver.Test1.Limits", "10/second");
        MCRConfigurationException mcrConfigurationException1 = assertThrows(MCRConfigurationException.class,
            () -> MCRRateLimitBuckets.getOrCreateBucket("Test1"));
        assertTrue(mcrConfigurationException1.getMessage().contains("10 tokens per second"));
        assertTrue(mcrConfigurationException1.getMessage().contains("Test1"));

        // Test negative value for token amount
        MCRConfiguration2.set("MCR.RateLimitResolver.Test2.Behavior", "block");
        MCRConfiguration2.set("MCR.RateLimitResolver.Test2.Limits", "-10/s");
        IllegalArgumentException illegalArgumentException =
            assertThrows(IllegalArgumentException.class, () -> MCRRateLimitBuckets.getOrCreateBucket("Test2"));
        assertTrue(illegalArgumentException.getMessage().contains("-10"));
        assertTrue(illegalArgumentException.getMessage().contains("capacity should be positive"));

        // Test non-number value for token amount
        MCRConfiguration2.set("MCR.RateLimitResolver.Test3.Behavior", "block");
        MCRConfiguration2.set("MCR.RateLimitResolver.Test3.Limits", "abc/s");
        NumberFormatException numberFormatException =
            assertThrows(NumberFormatException.class, () -> MCRRateLimitBuckets.getOrCreateBucket("Test3"));
        assertTrue(numberFormatException.getMessage().contains("abc"));

        // Test missing config
        MCRConfigurationException mcrConfigurationException2 = assertThrows(MCRConfigurationException.class,
            () -> MCRRateLimitBuckets.getOrCreateBucket("Test4"));
        assertTrue(mcrConfigurationException2.getMessage()
            .contains("Configuration property MCR.RateLimitResolver.Test4."));
    }

}
