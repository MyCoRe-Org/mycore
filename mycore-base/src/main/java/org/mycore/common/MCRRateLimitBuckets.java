package org.mycore.common;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.local.LocalBucketBuilder;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the {@link Bucket Buckets} for different configurations.
 * A configuration for a bucket and specific configuration ID looks like this:
 * <pre>
 * MCR.RateLimitResolver.&lt;configID&gt;.Behavior=[block/error/empty]
 * MCR.RateLimitResolver.&lt;configID&gt;.Limits=&lt;limit&gt;/&lt;timeunit&gt;
 * </pre>
 * Ex:<p>
 * MCR.RateLimitResolver.Scopus.Behavior=error<p>
 * MCR.RateLimitResolver.Scopus.Limits=6/s
 * <p>
 * Available time units are: M=months, D=days, h=hours, min=minutes, s=seconds
 * More than one time limit can be configured separated by comma.
 * The behavior defines if a downstream URI access should:
 * <ol>
 *     <li>block: block downstream access until end of limit is reached</li>
 *     <li>error: throw an error explaining reached access limit</li>
 *     <li>empty: returns an empty Source-Object upon reaching access limit</li>
 * </ol>

 */
public class MCRRateLimitBuckets {

    private static final String CONFIG_PREFIX = "MCR.RateLimitResolver.";

    private static final Map<String, Bucket> EXISTING_BUCKETS = new ConcurrentHashMap<>();

    private static String CONFIG_ID;

    /**
     * Clears the list of all buckets already created.
     */
    public static void clearAllBuckets() {
        EXISTING_BUCKETS.clear();
    }

    /**
     * Returns a {@link Bucket} using a specific configuration-ID. If a bucket was created before, it is returned,
     * else the bucket is newly created. If the configuration for an ID is missing,
     * a {@link MCRConfigurationException} is thrown.
     * @param configID the configuration-ID for a bucket
     * @return the bucket
     */
    public static Bucket getOrCreateBucket(String configID) {
        CONFIG_ID = configID;
        if (EXISTING_BUCKETS.containsKey(CONFIG_ID)) {
            return EXISTING_BUCKETS.get(CONFIG_ID);
        }
        final String dsConfigLimits = MCRConfiguration2.getStringOrThrow(CONFIG_PREFIX + CONFIG_ID + ".Limits");

        final HashMap<String, Long> limitMap = Arrays.stream(dsConfigLimits.split(",")).collect(
            HashMap::new, (map, str) -> map.put(str.split("/")[1].trim(),
                Long.parseLong(str.split("/")[0].trim())),
            HashMap::putAll);
        final Bucket bucket = createNewBucket(limitMap);
        EXISTING_BUCKETS.put(CONFIG_ID, bucket);
        return bucket;
    }

    /**
     * Creates a bucket using a map of limits and the configured ID. The used bucket refill-strategy is "intervally".
     * This means the whole amount of tokens is refilled after the whole time period has elapsed.
     * @param limitMap a map of time units and the corresponding limit/amount of tokens.
     * @return the created bucket
     */
    private static Bucket createNewBucket(HashMap<String, Long> limitMap) {
        final LocalBucketBuilder builder = Bucket.builder();
        for (Map.Entry<String, Long> entry : limitMap.entrySet()) {
            final String unit = entry.getKey();
            final Long amount = entry.getValue();
            builder.addLimit(limit -> limit.capacity(amount).refillIntervally(amount, getDuration(unit, amount)));
        }
        return builder.build();
    }

    /**
     * Helper method to determine the duration until a bucket refill for a given time unit.
     * @param unit the time unit in String-format
     * @param amount amount of tokens for a bucket
     * @return the duration until a bucket refill should happen
     */
    private static Duration getDuration(String unit, long amount) {
        switch (unit.toLowerCase(Locale.ROOT)) {
            case "m" -> {
                return Duration.ofDays(30);
            }
            case "d" -> {
                return Duration.ofDays(1);
            }
            case "h" -> {
                return Duration.ofHours(1);
            }
            case "min" -> {
                return Duration.ofMinutes(1);
            }
            case "s" -> {
                return Duration.ofSeconds(1);
            }
            default -> throw new MCRConfigurationException("The configuration \"" + amount + " tokens per " + unit +
                "\" for the ID \"" + CONFIG_ID + "\" is malformed. No time unit could be identified.");
        }
    }
}
