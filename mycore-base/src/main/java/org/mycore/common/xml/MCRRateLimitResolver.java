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

import java.util.concurrent.TimeUnit;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRRateLimitBuckets;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

/**
 * URI-Resolver that can limit the processing of downstream URI-Resolver operations.
 * Format ist "ratelimit:&lt;configID&gt;:&lt;anyMyCoReURI&gt;". Specific rate limits can be configured in
 * {@link Bucket Buckets}, see also {@link MCRRateLimitBuckets}.
 */
public class MCRRateLimitResolver implements URIResolver {

    private static final String CONFIG_PREFIX = "MCR.RateLimitResolver.";

    private static final Logger LOGGER = LogManager.getLogger(MCRRateLimitResolver.class);

    /**
     * Expects a configuration of the rate limit of a specific configID. Checks if
     * limit is reached and handles the configured behavior upon reaching it.
     * Resolves remaining URI if limit is not yet reached.
     * @param href An href attribute, which may be relative or absolute.
     * @param base The base URI against which the first argument will be made
     * absolute if the absolute URI is required.
     *
     * @return the {@link Source}-object of downstream processing
     * @throws TransformerException in case resolving of downstream processing leads to an error
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        final String subHref = href.substring(href.indexOf(":") + 1);
        final String configID = subHref.substring(0, subHref.indexOf(':'));
        final String resolvedHref = subHref.substring(subHref.indexOf(":") + 1);
        RateLimitBehavior behaviorConfig;
        try {
            behaviorConfig = RateLimitBehavior.fromValue(MCRConfiguration2.getStringOrThrow(
                CONFIG_PREFIX + configID + ".Behavior"));
        } catch (IllegalArgumentException ex) {
            throw new MCRConfigurationException("The behavior for ID " + configID +
                " is not correctly configured", ex);
        }
        Bucket currentRateLimit = MCRRateLimitBuckets.getOrCreateBucket(configID);
        final BucketConfig bucketConfig = new BucketConfig(configID, behaviorConfig, currentRateLimit);

        if (behaviorConfig.equals(RateLimitBehavior.BLOCK)) {
            try {
                currentRateLimit.asBlocking().consume(1);
                return MCRURIResolver.instance().resolve(resolvedHref, base);
            } catch (InterruptedException e) {
                return probeAccessLimit(resolvedHref, base, bucketConfig);
            }
        } else {
            return probeAccessLimit(resolvedHref, base, bucketConfig);
        }
    }

    /**
     * Tries to consume a token from the configured bucket. If successful, the remaining URI is resolved.
     * If all tokens are already consumed, the error is handled.
     * @param href the remaining URI to be resolved
     * @param base the base path of the URI
     * @param config the configured Bucket
     * @return the {@link Source}-object of the resolved URI
     * @throws TransformerException in case resolving of remaining URI leads to an error
     */
    private Source probeAccessLimit(String href, String base, BucketConfig config) throws TransformerException {
        ConsumptionProbe probe = config.bucket().tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            LOGGER.debug("There are " + probe.getRemainingTokens() + " accesses remaining");
            return MCRURIResolver.instance().resolve(href, base);
        } else {
            return handleError(probe, config);
        }
    }

    /**
     * Handles the behavior of a bucket once the limit is reached.
     * @param probe contains information about bucket state
     * @param config the configured Bucket
     * @return an empty {@link Source} object if {@link RateLimitBehavior#EMPTY} is configured
     */
    private Source handleError(ConsumptionProbe probe, BucketConfig config) {
        if (config.behavior().equals(RateLimitBehavior.ERROR)) {
            throw new MCRException("Data source " + config.configId() + " access limit reached. " +
                "Access to data source not possible. Try again in " +
                TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + " seconds.");
        } else {
            StreamSource emptySource = new StreamSource();
            emptySource.setSystemId("");
            return emptySource;
        }
    }

    /**
     * The behavior of a {@link Bucket} if the configured rate limit is reached. Possible values are:
     * <ul>
     *     <li>{@link #BLOCK}: blocks further processing until bucket is refilled</li>
     *     <li>{@link #ERROR}: an exception is thrown</li>
     *     <li>{@link #EMPTY}: an empty Source object is returned</li>
     * </ul>
     */
    public enum RateLimitBehavior {
        BLOCK("block"),
        ERROR("error"),
        EMPTY("empty");

        private final String value;

        RateLimitBehavior(final String value) {
            this.value = value;
        }

        public static RateLimitBehavior fromValue(final String value) {
            for (RateLimitBehavior behavior : values()) {
                if (behavior.value.equals(value)) {
                    return behavior;
                }
            }
            throw new IllegalArgumentException("The value is not a valid type of enum RateLimitBehavior: " + value);
        }
    }

    /**
     * read-only-object for handling information about a bucket
     */
    private record BucketConfig(String configId, RateLimitBehavior behavior, Bucket bucket) {
    }
}
