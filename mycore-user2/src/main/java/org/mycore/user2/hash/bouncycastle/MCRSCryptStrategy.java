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

import org.bouncycastle.crypto.generators.SCrypt;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.user2.hash.MCRPasswordCheckStrategy;
import org.mycore.user2.hash.MCRPasswordCheckStrategyBase;

import static org.mycore.user2.hash.MCRPasswordCheckUtils.fixedEffortEquals;

/**
 * {@link MCRSCryptStrategy} is n implementation of {@link MCRPasswordCheckStrategy} that
 * uses the SCrypt algorithm.
 * <p>
 * The salt and the hash are returned as hex encoded strings.
 * <p>
 * The verification result will be marked as outdated if the salt size or the hash size doesn't equal the
 * expected value.
 * <p>
 * Changes to the cost, the block size or the parallelism will result in deviating hashes and therefore prevent
 * the successful verification of existing hashes, even if the correct password is supplied.
 */
@MCRConfigurationProxy(proxyClass = MCRSCryptStrategy.Factory.class)
public class MCRSCryptStrategy extends MCRPasswordCheckStrategyBase {

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private final int saltSizeBytes;

    private final int hashSizeBytes;

    private final int cost;

    private final int blockSize;

    private final int parallelism;


    public MCRSCryptStrategy(int saltSizeBytes, int hashSizeBytes,
                             int cost, int blockSize, int parallelism) {
        this.saltSizeBytes = saltSizeBytes;
        this.hashSizeBytes = hashSizeBytes;
        this.cost = cost;
        this.blockSize = blockSize;
        this.parallelism = parallelism;
    }

    @Override
    public String invariableConfigurationString() {
        return "c=" + cost + "/bs=" + blockSize + "/p=" + parallelism;
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

        @MCRProperty(name = "SaltSizeBytes")
        public String saltSizeBytes;

        @MCRProperty(name = "HashSizeBytes")
        public String hashSizeBytes;

        @MCRProperty(name = "Cost")
        public String cost;

        @MCRProperty(name = "BlockSize")
        public String blockSize;

        @MCRProperty(name = "Parallelism")
        public String parallelism;

        @Override
        public MCRSCryptStrategy get() {
            return new MCRSCryptStrategy(Integer.parseInt(saltSizeBytes), Integer.parseInt(hashSizeBytes),
                Integer.parseInt(cost), Integer.parseInt(blockSize), Integer.parseInt(parallelism));
        }

    }

}
