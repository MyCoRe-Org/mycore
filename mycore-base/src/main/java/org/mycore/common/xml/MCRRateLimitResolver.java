package org.mycore.common.xml;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.util.concurrent.TimeUnit;

/**
 * URI-Resolver that can limit the processing of downstream URI-Resolver operations.
 * Format ist "ratelimit:&lt;configID&gt;:&lt;anyMyCoReURI&gt;". Specific rate limits can be configured in
 * {@link Bucket Buckets}, see also {@link MCRRateLimitBuckets}.
 */
public class MCRRateLimitResolver implements URIResolver {
    private RateLimitBehavior dsConfigBehavior;

    private String configID;

    private Bucket currentRateLimit;

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
        href = href.substring(href.indexOf(":") + 1);
        configID = href.substring(0, href.indexOf(':'));
        href = href.substring(href.indexOf(":") + 1);
        try {
            dsConfigBehavior = RateLimitBehavior.fromValue(MCRConfiguration2.getStringOrThrow(
                CONFIG_PREFIX + configID + ".Behavior"));
        } catch (IllegalArgumentException ex) {
            throw new MCRConfigurationException("The behavior for ID " + configID +
                " is not correctly configured", ex);
        }
        currentRateLimit = MCRRateLimitBuckets.getOrCreateBucket(configID);

        if (dsConfigBehavior.equals(RateLimitBehavior.BLOCK)) {
            try {
                currentRateLimit.asBlocking().consume(1);
                return MCRURIResolver.instance().resolve(href, base);
            } catch (InterruptedException e) {
                return probeAccessLimit(href, base);
            }
        } else {
            return probeAccessLimit(href, base);
        }
    }

    /**
     * Tries to consume a token from the configured bucket. If successful, the remaining URI is resolved.
     * If all tokens are already consumed, the error is handled.
     * @param href the remaining URI to be resolved
     * @param base the base path of the URI
     * @return the {@link Source}-object of the resolved URI
     * @throws TransformerException in case resolving of remaining URI leads to an error
     */
    private Source probeAccessLimit(String href, String base) throws TransformerException {
        ConsumptionProbe probe = currentRateLimit.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            LOGGER.debug("There are " + probe.getRemainingTokens() + "accesses remaining");
            return MCRURIResolver.instance().resolve(href, base);
        } else {
            return handleError(dsConfigBehavior, probe);
        }
    }

    /**
     * Handles the behavior of a bucket once the limit is reached.
     * @param behavior the {@link RateLimitBehavior} of the bucket configuration
     * @param probe contains information about bucket state
     * @return an empty {@link Source} object if {@link RateLimitBehavior#EMPTY} is configured
     */
    private Source handleError(RateLimitBehavior behavior, ConsumptionProbe probe) {
        if (behavior.equals(RateLimitBehavior.ERROR)) {
            throw new MCRException("Data source " + configID + " access limit reached. " +
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
            for (RateLimitBehavior behavior : RateLimitBehavior.values()) {
                if (behavior.value.equals(value)) {
                    return behavior;
                }
            }
            throw new IllegalArgumentException("The value is not a valid type of enum RateLimitBehavior: " + value);
        }
    }
}
