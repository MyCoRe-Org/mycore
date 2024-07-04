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
import java.util.Arrays;
import java.util.HexFormat;
import java.util.function.Supplier;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * {@link MCRPBKDF2Strategy} is n implementation of {@link MCRPasswordCheckStrategy} that
 * uses the PKKDF2 algorithm.
 * <p>
 * The salt is stored as a hex encoded String. The verification result will be marked as outdated if the size of
 * the salt doesn't equal the expected size.
 */
@MCRConfigurationProxy(proxyClass = MCRPBKDF2Strategy.Factory.class)
public class MCRPBKDF2Strategy extends MCRPasswordCheckStrategyBase {

    public static final HexFormat HEX_FORMAT = HexFormat.of();

    private final int saltSizeBytes;

    private final int hashSizeBytes;

    private final int iterations;

    public MCRPBKDF2Strategy(int saltSizeBytes, int hashSizeBytes, int iterations) {
        this.saltSizeBytes = saltSizeBytes;
        this.hashSizeBytes = hashSizeBytes;
        this.iterations = iterations;
    }

    @Override
    protected PasswordCheckData doCreate(SecureRandom random, String password) throws Exception {

        byte[] salt = random.generateSeed(saltSizeBytes);
        byte[] hash = getHash(salt, password);

        return new PasswordCheckData(HEX_FORMAT.formatHex(salt), HEX_FORMAT.formatHex(hash));
    }

    @Override
    protected PasswordCheckResult<Boolean> doVerify(PasswordCheckData data, String password) throws Exception {

        byte[] salt = HEX_FORMAT.parseHex(data.salt());
        byte[] hash = getHash(salt, password);

        boolean verified = Arrays.equals(HEX_FORMAT.parseHex(data.hash()), hash);
        boolean deprecated = data.salt().length() != saltSizeBytes;

        return new PasswordCheckResult<>(verified, deprecated);

    }

    private byte[] getHash(byte[] salt, String password) throws Exception {

        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, hashSizeBytes * 8);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
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
