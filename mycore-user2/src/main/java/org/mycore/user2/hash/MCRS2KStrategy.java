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

package org.mycore.user2.hash;

import static org.mycore.user2.hash.MCRPasswordCheckUtils.fixedEffortEquals;
import static org.mycore.user2.hash.MCRPasswordCheckUtils.probeHashAlgorithm;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

import com.google.common.primitives.Bytes;

/**
 * {@link MCRS2KStrategy} is an implementation of {@link MCRPasswordCheckStrategy} that uses the iterated and
 * salted String-to-Key (S2K) algorithm describes in RFC 4880 (OpenPGP Message Format).
 * <p>
 * The salt is returned as a hex encoded string. The hash is returned as a hex encoded string.
 * <p>
 * The following configuration options are available, if configured automatically:
 * <ul>
 * <li> The configuration suffix {@link MCRS2KStrategy#SALT_SIZE_BYTES_KEY} can be used to specify the size of
 * generated salt values in bytes.
 * <li> The configuration suffix {@link MCRS2KStrategy#HASH_SIZE_BYTES_KEY} can be used to specify the size of
 * generated hash values in bytes.
 * <li> The configuration suffix {@link MCRS2KStrategy#HASH_ALGORITHM_KEY} can be used to specify the hash algorithm
 * to be used.
 * <li> The configuration suffix {@link MCRS2KStrategy#COUNT_KEY} can be used to specify the count parameter the
 * determines the number of hashed bytes.
 * </ul>
 * Example:
 * <pre>
 * [...].Class=org.mycore.user2.hash.MCRS2KStrategy
 * [...].SaltSizeBytes=16
 * [...].HashSizeBytes=32
 * [...].HashAlgorithm=SHA256
 * [...].Count=275
 * </pre>
 * Changes to the hash algorithm or count parameter will result in deviating hashes and therefore prevent the
 * successful verification of existing hashes, even if the correct password is supplied. Changes to the salt size or
 * the hash size will not prevent verification, but successful verification results will be marked as outdated.
 */
@MCRConfigurationProxy(proxyClass = MCRS2KStrategy.Factory.class)
public class MCRS2KStrategy extends MCRPasswordCheckStrategyBase {

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    public static final String SALT_SIZE_BYTES_KEY = "SaltSizeBytes";

    public static final String HASH_SIZE_BYTES_KEY = "HashSizeBytes";

    public static final String HASH_ALGORITHM_KEY = "HashAlgorithm";

    public static final String COUNT_KEY = "Count";

    private final int saltSizeBytes;

    private final int hashSizeBytes;

    private final String hashAlgorithm;

    private final int count;

    public MCRS2KStrategy(int saltSizeBytes, int hashSizeBytes, String hashAlgorithm, int count) {
        if (saltSizeBytes < 1) {
            throw new IllegalArgumentException("Salt size [bytes] must be positive, got " + saltSizeBytes);
        }
        this.saltSizeBytes = saltSizeBytes;
        if (hashSizeBytes < 1) {
            throw new IllegalArgumentException("Hash size [bytes] must be positive, got " + hashSizeBytes);
        }
        this.hashSizeBytes = hashSizeBytes;
        this.hashAlgorithm = Objects.requireNonNull(hashAlgorithm, "Hash algorithm must not be null");
        probeHashAlgorithm(hashAlgorithm);
        if (count < 1) {
            throw new IllegalArgumentException("Count must be positive, got " + count);
        }
        this.count = count;
    }

    @Override
    public String unmodifiableConfigurationHint() {
        return "ha=" + hashAlgorithm + "/c=" + count;
    }

    @Override
    protected PasswordCheckData doCreate(SecureRandom random, String password) throws Exception {

        byte[] salt = random.generateSeed(saltSizeBytes);
        byte[] hash = getHash(salt, hashSizeBytes, password);

        return new PasswordCheckData(HEX_FORMAT.formatHex(salt), HEX_FORMAT.formatHex(hash));

    }

    @Override
    protected PasswordCheckResult<Boolean> doVerify(PasswordCheckData data, String password) throws Exception {

        byte[] checkSalt = HEX_FORMAT.parseHex(data.salt());
        byte[] checkHash = HEX_FORMAT.parseHex(data.hash());
        byte[] hash = getHash(checkSalt, checkHash.length, password);

        boolean verified = fixedEffortEquals(HEX_FORMAT.parseHex(data.hash()), hash);
        boolean deprecated = checkSalt.length != saltSizeBytes || checkHash.length != hashSizeBytes;

        return new PasswordCheckResult<>(verified, deprecated);

    }

    private byte[] getHash(byte[] salt, int hashSizeBytes, String password) throws Exception {

        // see definition of EXPBIAS in section 3.7.1.3. of RFC 4880 for original formula
        long numberOfBytes = ((long) (16 + (count & 15)) << ((count >> 4) + 6));
        byte[] source = Bytes.concat(salt, password.getBytes(StandardCharsets.UTF_8));

        MessageDigest digest = MessageDigest.getInstance(hashAlgorithm);
        int digestLength = digest.getDigestLength();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int hashIterations = hashSizeBytes / digestLength;
        for (int i = 0; i < hashIterations; i++) {
            buffer.write(getHash(digest, source, numberOfBytes, i));
            digest.reset();
        }
        buffer.write(getHash(digest, source, numberOfBytes, hashIterations), 0,
            hashSizeBytes - hashIterations * digestLength);

        return buffer.toByteArray();

    }

    private byte[] getHash(MessageDigest digest, byte[] source, long numberOfBytes, int preloadCount) {

        for (int i = 0; i < preloadCount; i++) {
            digest.update((byte) 0);
        }

        long iterations = numberOfBytes / source.length;
        for (long i = 0; i < iterations; i++) {
            digest.update(source);
        }
        digest.update(source, 0, (int) (numberOfBytes - iterations * source.length));

        return digest.digest();

    }

    public static class Factory implements Supplier<MCRS2KStrategy> {

        @MCRProperty(name = SALT_SIZE_BYTES_KEY)
        public String saltSizeBytes;

        @MCRProperty(name = HASH_SIZE_BYTES_KEY)
        public String hashSizeBytes;

        @MCRProperty(name = HASH_ALGORITHM_KEY)
        public String hashAlgorithm;

        @MCRProperty(name = COUNT_KEY)
        public String count;

        @Override
        public MCRS2KStrategy get() {
            return new MCRS2KStrategy(Integer.parseInt(saltSizeBytes), Integer.parseInt(hashSizeBytes),
                hashAlgorithm, Integer.parseInt(count));
        }

    }

}
