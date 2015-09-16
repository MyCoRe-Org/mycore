/**
 * 
 */
package org.mycore.frontend.support;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An implementation of SecureToken V2 used by "Wowza Streaming Engine".
 * <p>
 * A description of the algorithm:<br/>
 * <ol>
 * <li>A string is constructed by combining <code>contentPath</code>,'?' and all <strong>alphabetically sorted</strong>
 * parameters consisting of <code>ipAddress</code>, <code>sharedSecret</code> and any <code>queryParameters</code></li>
 * <li>Generate an <code>SHA-256</code> hash of that string builded in step 1 and {@link StandardCharsets#UTF_8}.</li>
 * <li>Generate a {@link Base64} encoded string of the digest of step 2.</li>
 * <li>replace <code>'+'</code> by <code>'-'</code> and <code>'/'</code> by <code>'_'</code> to make it a safe parameter
 * value.</li>
 * </ol>
 * </p>
 * 
 * @author Thomas Scheffler (yagee)
 * @see <a href="https://mycore.atlassian.net/browse/MCR-1058">JIRA Ticket MCR-1058</a>
 */
public class MCRSecureTokenV2 {

    private String contentPath, ipAddress, sharedSecret, hash;

    private String[] queryParameters;

    public MCRSecureTokenV2(String contentPath, String ipAddress, String sharedSecret, String... queryParameters) {
        this.contentPath = Objects.requireNonNull(contentPath, "'contentPath' may not be null");
        this.ipAddress = Objects.requireNonNull(ipAddress, "'ipAddress' may not be null");
        this.sharedSecret = Objects.requireNonNull(sharedSecret, "'sharedSecret' may not be null");
        this.queryParameters = queryParameters;
        buildHash();
    }

    private void buildHash() {
        String forHashing = Stream.concat(Stream.of(ipAddress, sharedSecret), Arrays.stream(queryParameters))
            .sorted()
            .collect(Collectors.joining("&", contentPath + "?", ""));
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);//should never happen for 'SHA-256'
        }
        digest.update(forHashing.getBytes(StandardCharsets.UTF_8));
        byte[] sha256 = digest.digest();
        hash = Base64.getEncoder()
            .encodeToString(sha256)
            .chars()
            .map(x -> {
                switch (x) {
                    case '+':
                        return '-';
                    case '/':
                        return '_';
                    default:
                        return x;
                }
            })
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
    }

    public String getHash() {
        return hash;
    }

    /**
     * Same as calling {@link #toURL(String, String, String)} with <code>suffix=""</code>.
     */
    public URL toURL(String baseURL, String hashParameterName) throws MalformedURLException {
        return toURL(baseURL, "", hashParameterName);
    }

    /**
     * Constructs an URL by using all information from the
     * {@link MCRSecureTokenV2#MCRSecureTokenV2(String, String, String, String...) constructor} except
     * <code>ipAddress</code> and <code>sharedSecret</code> and the supplied parameters.
     * 
     * @param baseURL
     *            a valid and absolute base URL
     * @param suffix
     *            is appended to the <code>contentPath<
     * &#64;param hashParameterName the name of the query parameter that holds the hash value
     * @return an absolute URL consisting of all elements as stated above and <code>queryParameters</code> in the
     *         <strong>given order</strong> appended by the hash parameter and the hash value from {@link #getHash()}.
     * @throws MalformedURLException
     *             if baseURL is not a valid URL
     */
    public URL toURL(String baseURL, String suffix, String hashParameterName) throws MalformedURLException {
        Objects.requireNonNull(suffix, "'suffix' may not be null");
        Objects.requireNonNull(hashParameterName, "'hashParameterName' may not be null");
        if (hashParameterName.isEmpty()) {
            throw new IllegalArgumentException("'hashParameterName' may not be empty");
        }
        URL context = new URL(baseURL);
        URL completeURL = new URL(context, Stream
            .concat(Arrays.stream(queryParameters), Stream.of(hashParameterName + "=" + hash))
            .collect(Collectors.joining("&", contentPath + suffix + "?", "")));
        return completeURL;
    }

    @Override
    public int hashCode() {
        return getHash().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MCRSecureTokenV2 other = (MCRSecureTokenV2) obj;
        if (!hash.equals(other.hash)) {
            return false;
        }
        if (!contentPath.equals(other.contentPath)) {
            return false;
        }
        if (!ipAddress.equals(other.ipAddress)) {
            return false;
        }
        if (!sharedSecret.equals(other.sharedSecret)) {
            return false;
        }
        if (!Arrays.equals(queryParameters, other.queryParameters)) {
            return false;
        }
        return true;
    }

}
