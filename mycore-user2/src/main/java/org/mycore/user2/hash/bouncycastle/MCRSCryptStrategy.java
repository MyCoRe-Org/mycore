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

package org.mycore.user2.hash.bouncycastle;

import static org.mycore.user2.hash.MCRPasswordCheckUtils.fixedEffortEquals;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.function.Supplier;

import org.bouncycastle.crypto.generators.SCrypt;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.user2.hash.MCRPasswordCheckStrategy;
import org.mycore.user2.hash.MCRPasswordCheckStrategyBase;

/**
 * {@link MCRSCryptStrategy} is an implementation of {@link MCRPasswordCheckStrategy} that uses the SCrypt algorithm.
 * <p>
 * The version and salt are encoded in the hash using the Modular Crypt Format (MCF) for SCrypt. No explicit salt
 * values are generated.
 * <p>
 * The salt is returned as a hex encoded string. The hash is returned as a hex encoded string.
 * <ul>
 * <li> The configuration suffix {@link MCRSCryptStrategy#SALT_SIZE_BYTES_KEY} can be used to specify the size of
 * generated salt values in bytes.
 * <li> The configuration suffix {@link MCRSCryptStrategy#HASH_SIZE_BYTES_KEY} can be used to specify the size of
 * generated hash values in bytes.
 * <li> The configuration suffix {@link MCRSCryptStrategy#PARALLELISM_KEY} can be used to specify the parallelization
 * value.
 * <li> The configuration suffix {@link MCRSCryptStrategy#BLOCK_SIZE_KEY} can be used to influences the amount of
 * memory to be used.
 * <li> The configuration suffix {@link MCRSCryptStrategy#COST_KEY} can be used to specify the cost parameter the
 * CPU/memory cost parameter.
 * </ul>
 * Example:
 * <pre>
 * [...].Class=org.mycore.user2.hash.bouncycastle.MCRSCryptStrategy
 * [...].SaltSizeBytes=32
 * [...].HashSizeBytes=64
 * [...].Parallelism=1
 * [...].BlockSize=8
 * [...].Cost=17
 * </pre>
 * Changes to the parallelism value, block size or cost parameter will result in deviating hashes and therefore prevent
 * the successful verification of existing hashes, even if the correct password is supplied. Changes to the salt size
 * or the hash size will not prevent verification, but successful verification results will be marked as outdated.
 */
@MCRConfigurationProxy(proxyClass = MCRSCryptStrategy.Factory.class)
public class MCRSCryptStrategy extends MCRPasswordCheckStrategyBase {

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    public static final String SALT_SIZE_BYTES_KEY = "SaltSizeBytes";

    public static final String HASH_SIZE_BYTES_KEY = "HashSizeBytes";

    public static final String PARALLELISM_KEY = "Parallelism";

    public static final String BLOCK_SIZE_KEY = "BlockSize";

    public static final String COST_KEY = "Cost";

    private final int saltSizeBytes;

    private final int hashSizeBytes;

    private final int parallelism;

    private final int blockSize;

    private final int cost;

    public MCRSCryptStrategy(int saltSizeBytes, int hashSizeBytes, int parallelism, int blockSize, int cost) {
        if (saltSizeBytes < 1) {
            throw new IllegalArgumentException("Salt size [bytes] must be positive, got " + saltSizeBytes);
        }
        this.saltSizeBytes = saltSizeBytes;
        if (hashSizeBytes < 1) {
            throw new IllegalArgumentException("Hash size [bytes] must be positive, got " + hashSizeBytes);
        }
        this.hashSizeBytes = hashSizeBytes;
        if (parallelism < 1) {
            throw new IllegalArgumentException("Parallelism must be positive, got " + parallelism);
        }
        this.parallelism = parallelism;
        if (blockSize < 1) {
            throw new IllegalArgumentException("Block size must be positive, got " + blockSize);
        }
        this.blockSize = blockSize;
        if (cost < 1) {
            throw new IllegalArgumentException("Cost must be positive, got " + cost);
        }
        this.cost = cost;
    }

    @Override
    public String unmodifiableConfigurationHint() {
        return "p=" + parallelism + "/bs=" + blockSize + "/c=" + cost;
    }

    @Override
    protected PasswordCheckData doCreate(SecureRandom random, String password) {

        byte[] salt = random.generateSeed(saltSizeBytes);
        byte[] hash = getHash(salt, hashSizeBytes, password);

        return new PasswordCheckData(HEX_FORMAT.formatHex(salt), HEX_FORMAT.formatHex(hash));

    }

    @Override
    protected PasswordCheckResult<Boolean> doVerify(PasswordCheckData data, String password) {

        byte[] checkSalt = HEX_FORMAT.parseHex(data.salt());
        byte[] checkHash = HEX_FORMAT.parseHex(data.hash());
        byte[] hash = getHash(checkSalt, checkHash.length, password);

        boolean verified = fixedEffortEquals(HEX_FORMAT.parseHex(data.hash()), hash);
        boolean deprecated = checkSalt.length != saltSizeBytes || checkHash.length != hashSizeBytes;

        return new PasswordCheckResult<>(verified, deprecated);

    }

    private byte[] getHash(byte[] salt, int hashSizeBytes, String password) {

        byte[] bytes = password.getBytes(StandardCharsets.UTF_8);
        return SCrypt.generate(bytes, salt, 1 << cost, blockSize, parallelism, hashSizeBytes);

    }

    public static class Factory implements Supplier<MCRSCryptStrategy> {

        @MCRProperty(name = SALT_SIZE_BYTES_KEY)
        public String saltSizeBytes;

        @MCRProperty(name = HASH_SIZE_BYTES_KEY)
        public String hashSizeBytes;

        @MCRProperty(name = PARALLELISM_KEY)
        public String parallelism;

        @MCRProperty(name = BLOCK_SIZE_KEY)
        public String blockSize;

        @MCRProperty(name = COST_KEY)
        public String cost;

        @Override
        public MCRSCryptStrategy get() {
            return new MCRSCryptStrategy(Integer.parseInt(saltSizeBytes), Integer.parseInt(hashSizeBytes),
                Integer.parseInt(parallelism), Integer.parseInt(blockSize), Integer.parseInt(cost));
        }

    }

}
