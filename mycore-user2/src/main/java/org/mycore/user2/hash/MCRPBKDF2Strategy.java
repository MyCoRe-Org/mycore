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
import java.util.function.Supplier;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

import static org.mycore.user2.hash.MCRPasswordCheckUtils.fixedEffortEquals;

/**
 * {@link MCRPBKDF2Strategy} is n implementation of {@link MCRPasswordCheckStrategy} that
 * uses the PBKDF2 algorithm.
 * <p>
 * The salt and the hash are returned as hex encoded strings.
 * <p>
 * The verification result will be marked as outdated if the salt size or the hash size doesn't equal the
 * expected value.
 * <p>
 * Changes to the number of iterations will result in deviating hashes and therefore prevent the successful
 * verification of existing hashes, even if the correct password is supplied.
 */
@MCRConfigurationProxy(proxyClass = MCRPBKDF2Strategy.Factory.class)
public class MCRPBKDF2Strategy extends MCRPasswordCheckStrategyBase {

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private final int saltSizeBytes;

    private final int hashSizeBytes;

    private final int iterations;

    public MCRPBKDF2Strategy(int saltSizeBytes, int hashSizeBytes, int iterations) {
        this.saltSizeBytes = saltSizeBytes;
        this.hashSizeBytes = hashSizeBytes;
        this.iterations = iterations;
    }

    @Override
    public String invariableConfigurationString() {
        return "i=" + iterations;
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
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();

    }

    public static class Factory implements Supplier<MCRPBKDF2Strategy> {

        @MCRProperty(name = "SaltSizeBytes")
        public String saltSizeBytes;

        @MCRProperty(name = "HashSizeBytes")
        public String hashSizeBytes;

        @MCRProperty(name = "Iterations")
        public String iterations;

        @Override
        public MCRPBKDF2Strategy get() {
            return new MCRPBKDF2Strategy(Integer.parseInt(saltSizeBytes), Integer.parseInt(hashSizeBytes),
                Integer.parseInt(iterations));
        }

    }
    
}
