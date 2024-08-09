/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Supplier;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

import static org.mycore.user2.hash.MCRPasswordCheckUtils.fixedEffortEquals;
import static org.mycore.user2.hash.MCRPasswordCheckUtils.probeSecretKeyAlgorithm;

/**
 * {@link MCRPBKDF2Strategy} is an implementation of {@link MCRPasswordCheckStrategy} that uses the PBKDF2 algorithm.
 * <p>
 * The salt is returned as a hex encoded string. The hash is returned as a hex encoded string.
 * <p>
 * The following configuration options are available, if configured automatically:
 * <ul>
 * <li> The configuration suffix {@link MCRPBKDF2Strategy#SALT_SIZE_BYTES_KEY} can be used to specify the size of
 * generated salt values in bytes.
 * <li> The configuration suffix {@link MCRPBKDF2Strategy#HASH_SIZE_BYTES_KEY} can be used to specify the size of
 * generated hash values in bytes.
 * <li> The configuration suffix {@link MCRPBKDF2Strategy#HASH_ALGORITHM_KEY} can be used to specify the hash algorithm
 * to be used.
 * <li> The configuration suffix {@link MCRPBKDF2Strategy#ITERATIONS_KEY} can be used to specify the number of
 * iterations to be performed.
 * </ul>
 * Example:
 * <pre>
 * [...].Class=org.mycore.user2.hash.MCRPBKDF2Strategy
 * [...].SaltSizeBytes=16
 * [...].HashSizeBytes=32
 * [...].HashAlgorithm=SHA256
 * [...].Iterations=1000000
 * </pre>
 * Changes to the hash algorithm or the number of iterations will result in deviating hashes and therefore prevent the
 * successful verification of existing hashes, even if the correct password is supplied. Changes to the salt size or
 * the hash size will not prevent verification, but successful verification results will be marked as outdated.
 */
@MCRConfigurationProxy(proxyClass = MCRPBKDF2Strategy.Factory.class)
public class MCRPBKDF2Strategy extends MCRPasswordCheckStrategyBase {

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    public static final String SALT_SIZE_BYTES_KEY = "SaltSizeBytes";

    public static final String HASH_SIZE_BYTES_KEY = "HashSizeBytes";

    public static final String HASH_ALGORITHM_KEY = "HashAlgorithm";

    public static final String ITERATIONS_KEY = "Iterations";

    private final int saltSizeBytes;

    private final int hashSizeBytes;

    private final String hashAlgorithm;

    private final int iterations;

    public MCRPBKDF2Strategy(int saltSizeBytes, int hashSizeBytes, String hashAlgorithm, int iterations) {
        if (saltSizeBytes < 1) {
            throw new IllegalArgumentException("Salt size [bytes] must be positive, got " + saltSizeBytes);
        }
        this.saltSizeBytes = saltSizeBytes;
        if (hashSizeBytes < 1) {
            throw new IllegalArgumentException("Hash size [bytes] must be positive, got " + hashSizeBytes);
        }
        this.hashSizeBytes = hashSizeBytes;
        this.hashAlgorithm = Objects.requireNonNull(hashAlgorithm, "Hash algorithm must not be null");
        probeSecretKeyAlgorithm(pdkdf2Algorithm(hashAlgorithm));
        if (iterations < 1) {
            throw new IllegalArgumentException("Iterations must be positive, got " + iterations);
        }
        this.iterations = iterations;
    }

    @Override
    public String unmodifiableConfigurationHint() {
        return "ha=" + hashAlgorithm + "/i=" + iterations;
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

        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, hashSizeBytes * 8);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(pdkdf2Algorithm(hashAlgorithm));
        return factory.generateSecret(spec).getEncoded();

    }

    private static String pdkdf2Algorithm(String hashAlgorithm) {
        return "PBKDF2WithHmac" + hashAlgorithm;
    }

    public static class Factory implements Supplier<MCRPBKDF2Strategy> {

        @MCRProperty(name = SALT_SIZE_BYTES_KEY)
        public String saltSizeBytes;

        @MCRProperty(name = HASH_SIZE_BYTES_KEY)
        public String hashSizeBytes;

        @MCRProperty(name = HASH_ALGORITHM_KEY)
        public String hashAlgorithm;

        @MCRProperty(name = ITERATIONS_KEY)
        public String iterations;

        @Override
        public MCRPBKDF2Strategy get() {
            return new MCRPBKDF2Strategy(Integer.parseInt(saltSizeBytes), Integer.parseInt(hashSizeBytes),
                hashAlgorithm, Integer.parseInt(iterations));
        }

    }

}
