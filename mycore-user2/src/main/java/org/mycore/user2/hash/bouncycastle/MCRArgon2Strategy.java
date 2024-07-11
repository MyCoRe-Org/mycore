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

package org.mycore.user2.hash.bouncycastle;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.function.Supplier;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.user2.hash.MCRPasswordCheckStrategy;
import org.mycore.user2.hash.MCRPasswordCheckStrategyBase;

import static org.mycore.user2.hash.MCRPasswordCheckUtils.fixedEffortEquals;

/**
 * {@link MCRArgon2Strategy} is an implementation of {@link MCRPasswordCheckStrategy} that uses the Argon2 algorithm.
 * <p>
 * The version and salt are encoded in the hash using the Modular Crypt Format (MCF) for Argon2. No explicit salt
 * values are generated.
 * <p>
 * The salt is returned as a hex encoded string. The hash is returned as a hex encoded string.
 * <ul>
 * <li> The configuration suffix {@link MCRArgon2Strategy#SALT_SIZE_BYTES_KEY} can be used to specify the size of
 * generated salt values in bytes.
 * <li> The configuration suffix {@link MCRArgon2Strategy#HASH_SIZE_BYTES_KEY} can be used to specify the size of
 * generated hash values in bytes.
 * <li> The configuration suffix {@link MCRArgon2Strategy#PARALLELISM_KEY} can be used to specify the parallelization
 * value.
 * <li> The configuration suffix {@link MCRArgon2Strategy#MEMORY_LIMIT_KILOBYTES_KEY} can be used to influences the
 * amount of memory to be used in kilobytes.
 * <li> The configuration suffix {@link MCRArgon2Strategy#ITERATIONS_KEY} can be used to specify the number of
 * iterations to be performed.
 * </ul>
 * Example:
 * <pre>
 * [...].Class=org.mycore.user2.hash.bouncycastle.MCRArgon2Strategy
 * [...].SaltSizeBytes=32
 * [...].HashSizeBytes=64
 * [...].Parallelism=1
 * [...].MemoryLimitKilobytes=66536
 * [...].Iterations=8
 * </pre>
 * This will generate salt values of length 32 and hashes of length 64, use 1 as the parallelism value, 66536
 * kilobytes as the memory limit and perform 8 iterations.
 * <p>
 * Changes to the parallelism value, memory limit or number of iterations will result in deviating hashes and therefore
 * prevent the successful verification of existing hashes, even if the correct password is supplied. Changes to the
 * salt size or the hash size will not prevent verification, but successful verification results will be marked as
 * outdated.
 */
@MCRConfigurationProxy(proxyClass = MCRArgon2Strategy.Factory.class)
public class MCRArgon2Strategy extends MCRPasswordCheckStrategyBase {

    public static final String SALT_SIZE_BYTES_KEY = "SaltSizeBytes";

    public static final String HASH_SIZE_BYTES_KEY = "HashSizeBytes";

    public static final String PARALLELISM_KEY = "Parallelism";

    public static final String MEMORY_LIMIT_KILOBYTES_KEY = "MemoryLimitKilobytes";

    public static final String ITERATIONS_KEY = "Iterations";

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private static final int TYPE = Argon2Parameters.ARGON2_id;

    private static final int VERSION = Argon2Parameters.ARGON2_VERSION_13;

    private final int saltSizeBytes;

    private final int hashSizeBytes;

    private final int parallelism;

    private final int memoryLimitKilobytes;

    private final int iterations;


    public MCRArgon2Strategy(int saltSizeBytes, int hashSizeBytes,
                             int parallelism, int memoryLimitKilobytes, int iterations) {
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
        if (memoryLimitKilobytes < 2 * parallelism) {
            throw new IllegalArgumentException("Memory limit [kilobytes] must be at least " + (2 * parallelism) +
                ", got " + memoryLimitKilobytes);
        }
        this.memoryLimitKilobytes = memoryLimitKilobytes;
        if (iterations < 1) {
            throw new IllegalArgumentException("Iterations must be positive, got " + iterations);
        }
        this.iterations = iterations;
    }

    @Override
    public String invariableConfiguration() {
        return "p=" + parallelism + "/ml=" + memoryLimitKilobytes + "/i=" + iterations;
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

        Argon2BytesGenerator generate = getGenerator(salt);
        byte[] hash = new byte[hashSizeBytes];
        generate.generateBytes(password.getBytes(StandardCharsets.UTF_8), hash);

        return hash;

    }

    private Argon2BytesGenerator getGenerator(byte[] salt) {

        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(TYPE)
            .withVersion(VERSION)
            .withIterations(iterations)
            .withMemoryAsKB(memoryLimitKilobytes)
            .withParallelism(parallelism)
            .withSalt(salt);

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(builder.build());

        return generator;

    }

    public static class Factory implements Supplier<MCRArgon2Strategy> {

        @MCRProperty(name = SALT_SIZE_BYTES_KEY)
        public String saltSizeBytes;

        @MCRProperty(name = HASH_SIZE_BYTES_KEY)
        public String hashSizeBytes;

        @MCRProperty(name = PARALLELISM_KEY)
        public String parallelism;

        @MCRProperty(name = MEMORY_LIMIT_KILOBYTES_KEY)
        public String memoryLimitKilobytes;

        @MCRProperty(name = ITERATIONS_KEY)
        public String iterations;

        @Override
        public MCRArgon2Strategy get() {
            return new MCRArgon2Strategy(Integer.parseInt(saltSizeBytes), Integer.parseInt(hashSizeBytes),
                Integer.parseInt(parallelism), Integer.parseInt(memoryLimitKilobytes), Integer.parseInt(iterations));
        }

    }

}
