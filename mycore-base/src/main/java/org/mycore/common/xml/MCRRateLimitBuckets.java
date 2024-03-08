package org.mycore.common.xml;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucketBuilder;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the {@link Bucket Buckets} for different configurations.
 * A configuration for a bucket and specific configuration ID looks like this:
 * <pre>
 * MCR.RateLimitResolver.&lt;configID&gt;.Behavior=[block/error/empty]</p>
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

    private static final HashMap<String, Bucket> EXISTING_BUCKETS = new HashMap<>();

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

        HashMap<String, Integer> limitMap = Arrays.stream(dsConfigLimits.split(",")).collect(
            HashMap::new, (map, str) -> map.put(str.split("/")[1].trim(),
                Integer.parseInt(str.split("/")[0].trim())),
            HashMap::putAll);
        final Bucket bucket = createNewBucket(limitMap);
        EXISTING_BUCKETS.put(CONFIG_ID, bucket);
        return bucket;
    }

    /**
     * Creates a bucket using a map of limits and the configured ID.
     * @param limitMap a map of time units and the corresponding limit/amount of tokens.
     * @return the created bucket
     */
    private static Bucket createNewBucket(HashMap<String, Integer> limitMap) {
        final LocalBucketBuilder builder = Bucket.builder();
        for (Map.Entry<String, Integer> entry : limitMap.entrySet()) {
            builder.addLimit(getBandwidth(entry.getKey(), entry.getValue()).withId(entry.getKey() + "-limit"));
        }
        return builder.build();
    }

    /**
     * To create a bucket, limits are added using one or multiple {@link Bandwidth Bandwidths}.
     * This method creates and returns a bandwidth using the amount of bucket-tokens per time unit.
     * The used bucket refill-strategy is "intervally". This means the whole amount of tokens is
     * refilled after the whole time period has elapsed.
     * @param unit the time unit used
     * @param amount the amount of tokens
     * @return the created bandwidth
     */
    private static Bandwidth getBandwidth(String unit, int amount) {
        Duration duration;
        switch (unit.toLowerCase()) {
            case "m" -> {
                duration = Duration.ofDays(30);
            }
            case "d" -> {
                duration = Duration.ofDays(1);
            }
            case "h" -> {
                duration = Duration.ofHours(1);
            }
            case "min" -> {
                duration = Duration.ofMinutes(1);
            }
            case "s" -> {
                duration = Duration.ofSeconds(1);
            }
            default -> throw new MCRConfigurationException("The configuration \"" + amount + " tokens per " + unit +
                "\" for the ID \"" + CONFIG_ID + "\" is malformed. No time unit can be identified.");
        }
        final Refill refill = Refill.intervally(amount, duration);
        return Bandwidth.classic(amount, refill);
    }
}
