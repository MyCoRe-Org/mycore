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
 * {@link MCRArgon2Strategy} is n implementation of {@link MCRPasswordCheckStrategy} that
 * uses the Argon2 algorithm.
 * <p>
 * The salt and the hash are returned as hex encoded strings.
 * <p>
 * The verification result will be marked as outdated if the salt size or the hash size doesn't equal the
 * expected value.
 * <p>
 * Changes to the number of iterations, the memory limit or the parallelism will result in deviating hashes
 * and therefore prevent the successful verification of existing hashes, even if the correct password is supplied.
 */
@MCRConfigurationProxy(proxyClass = MCRArgon2Strategy.Factory.class)
public class MCRArgon2Strategy extends MCRPasswordCheckStrategyBase {

    private static final int TYPE = Argon2Parameters.ARGON2_id;

    private static final int VERSION = Argon2Parameters.ARGON2_VERSION_13;

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private final int saltSizeBytes;

    private final int hashSizeBytes;

    private final int iterations;

    private final int memoryLimitKiloBytes;

    private final int parallelism;

    public MCRArgon2Strategy(int saltSizeBytes, int hashSizeBytes,
                             int iterations, int memoryLimitKiloBytes, int parallelism) {
        this.saltSizeBytes = saltSizeBytes;
        this.hashSizeBytes = hashSizeBytes;
        this.iterations = iterations;
        this.memoryLimitKiloBytes = memoryLimitKiloBytes;
        this.parallelism = parallelism;
    }

    @Override
    public String invariableConfiguration() {
        return "i=" + iterations + "/ml=" + memoryLimitKiloBytes + "/p=" + parallelism;
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
            .withMemoryAsKB(memoryLimitKiloBytes)
            .withParallelism(parallelism)
            .withSalt(salt);

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(builder.build());

        return generator;

    }

    public static class Factory implements Supplier<MCRArgon2Strategy> {

        @MCRProperty(name = "SaltSizeBytes")
        public String saltSizeBytes;

        @MCRProperty(name = "HashSizeBytes")
        public String hashSizeBytes;

        @MCRProperty(name = "Iterations")
        public String iterations;

        @MCRProperty(name = "MemoryLimitKiloBytes")
        public String memoryLimitKiloBytes;

        @MCRProperty(name = "Parallelism")
        public String parallelism;

        @Override
        public MCRArgon2Strategy get() {
            return new MCRArgon2Strategy(Integer.parseInt(saltSizeBytes), Integer.parseInt(hashSizeBytes),
                Integer.parseInt(iterations), Integer.parseInt(memoryLimitKiloBytes), Integer.parseInt(parallelism));
        }

    }

}
