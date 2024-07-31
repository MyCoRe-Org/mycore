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

import java.util.function.Supplier;

import org.mycore.common.MCRUtils;
import org.mycore.common.annotation.MCROutdated;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

import static org.mycore.user2.hash.MCRPasswordCheckUtils.probeHashAlgorithm;

/**
 * {@link MCRSHA1Strategy} is an implementation of {@link MCRPasswordCheckStrategy} that uses the SHA1 algorithm.
 * <p>
 * The salt is returned as a base 64 encoded string. The hash is returned as a hex encoded string.
 * <p>
 * The following configuration options are available, if configured automatically:
 * <ul>
 * <li> The configuration suffix {@link MCRSHA1Strategy#SALT_SIZE_BYTES_KEY} can be used to specify the size of
 * generated salt values in bytes.
 * <li> The configuration suffix {@link MCRSHA1Strategy#ITERATIONS_KEY} can be used to specify the number of
 * iterations to be performed.
 * </ul>
 * Example:
 * <pre>
 * [...].Class=org.mycore.user2.hash.MCRSHA1Strategy
 * [...].SaltSizeBytes=8
 * [...].Iterations=1000
 * </pre>
 * Changes to the number of iterations will result in deviating hashes and therefore prevent the successful
 * verification of existing hashes, even if the correct password is supplied. Changes to the salt size will not prevent
 * verification, but successful verification results will be marked as outdated.
 */
@MCROutdated
@MCRConfigurationProxy(proxyClass = MCRSHA1Strategy.Factory.class)
public class MCRSHA1Strategy extends MCRSaltedHashPasswordCheckStrategy {

    public static final String SALT_SIZE_BYTES_KEY = "SaltSizeBytes";

    public static final String ITERATIONS_KEY = "Iterations";

    private final int iterations;

    public MCRSHA1Strategy(int saltSizeBytes, int iterations) {
        super(saltSizeBytes);
        if (iterations < 1) {
            throw new IllegalArgumentException("Iterations must be positive, got " + iterations);
        }
        this.iterations = iterations;
        probeHashAlgorithm("SHA-1");
    }

    @Override
    public String unmodifiableConfigurationHint() {
        return "i=" + iterations;
    }

    @Override
    protected String doCreateSaltedHash(byte[] salt, String password) throws Exception {
        return MCRUtils.asSHA1String(iterations, salt, password);
    }

    public static class Factory implements Supplier<MCRSHA1Strategy> {

        @MCRProperty(name = SALT_SIZE_BYTES_KEY)
        public String saltSizeBytes;

        @MCRProperty(name = ITERATIONS_KEY)
        public String iterations;

        @Override
        public MCRSHA1Strategy get() {
            return new MCRSHA1Strategy(Integer.parseInt(saltSizeBytes), Integer.parseInt(iterations));
        }

    }

}
