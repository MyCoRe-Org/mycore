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

package org.mycore.common.xsl.uriresolver;

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
 * {@link URIResolver} that limits the rate of downstream URI resolver operations via token buckets.
 * <p>Rate limit buckets and their behavior are configured via:
 * <ul>
 *   <li>{@code MCR.RateLimitResolver.{configId}.Behavior}: one of {@code block}, {@code error}, or {@code empty}</li>
 * </ul>
 * See {@link MCRRateLimitBuckets} for bucket configuration.
 */
public class MCRRateLimitResolver implements URIResolver {

    private static final String CONFIG_PREFIX = "MCR.RateLimitResolver.";

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Checks the rate limit for the given config ID and resolves the target URI if the limit
     * has not been reached.
     * <p>Behavior when the limit is reached depends on the configured {@link RateLimitBehavior}:
     * {@code block} waits until a token is available, {@code error} throws an exception,
     * and {@code empty} returns an empty source.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{configId}:{anyMCRUri}
     * </pre>
     * <p>Example request:
     * <pre>
     *   ratelimit:myLimit:mcrobject:mcr_document_00000001
     * </pre>
     * <p>Example response on success: the resolved content of the target URI.
     * <p>Example response with {@code empty} behavior:
     * <pre>{@code
     *   (empty source)
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet, passed through to the delegated resolver
     * @return a {@link Source} wrapping the resolved content, or an empty source if the limit
     *         is reached and {@code empty} behavior is configured
     * @throws TransformerException if the target URI cannot be resolved
     * @throws MCRException if the limit is reached and {@code error} behavior is configured
     * @throws MCRConfigurationException if the behavior for the given config ID is not configured correctly
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        final String subHref = href.substring(href.indexOf(':') + 1);
        final String configID = subHref.substring(0, subHref.indexOf(':'));
        final String resolvedHref = subHref.substring(subHref.indexOf(':') + 1);
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
                return MCRURIResolver.obtainInstance().resolve(resolvedHref, base);
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
            LOGGER.debug(() -> "There are " + probe.getRemainingTokens() + " accesses remaining");
            return MCRURIResolver.obtainInstance().resolve(href, base);
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
