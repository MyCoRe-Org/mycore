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

import static org.mycore.user2.hash.MCRPasswordCheckUtils.probeHashAlgorithm;

import java.util.function.Supplier;

import org.mycore.common.MCRUtils;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * A {@link MCRSHA256Strategy} is a {@link MCRPasswordCheckStrategy} that uses the SHA-256 algorithm.
 * <p>
 * The salt is returned as a base 64 encoded string. The hash is returned as a hex encoded string.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRSHA256Strategy#SALT_SIZE_BYTES_KEY} can be used to
 * specify the size of generated salt values in bytes.
 * <li> The property suffix {@link MCRSHA256Strategy#ITERATIONS_KEY} can be used to
 * specify the number of iterations to be performed.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.user2.hash.MCRSHA256Strategy
 * [...].SaltSizeBytes=8
 * [...].Iterations=1000
 * </code></pre>
 * Changes to the number of iterations will result in deviating hashes and therefore prevent
 * the successful verification of existing hashes, even if the correct password is supplied.
 * <p>
 * Changes to the salt size will not prevent verification, but successful verification results
 * will be marked as outdated.
 */
@MCRConfigurationProxy(proxyClass = MCRSHA256Strategy.Factory.class)
public class MCRSHA256Strategy extends MCRSaltedHashPasswordCheckStrategyBase {

    public static final String SALT_SIZE_BYTES_KEY = "SaltSizeBytes";

    public static final String ITERATIONS_KEY = "Iterations";

    private final int iterations;

    public MCRSHA256Strategy(int saltSizeBytes, int iterations) {
        super(saltSizeBytes);
        if (iterations < 1) {
            throw new IllegalArgumentException("Iterations must be positive, got " + iterations);
        }
        this.iterations = iterations;
        probeHashAlgorithm("SHA-256");
    }

    @Override
    public String unmodifiableConfigurationHint() {
        return "i=" + iterations;
    }

    @Override
    protected String doCreateSaltedHash(byte[] salt, String password) throws Exception {
        return MCRUtils.asSHA256String(iterations, salt, password);
    }

    public static class Factory implements Supplier<MCRSHA256Strategy> {

        @MCRProperty(name = SALT_SIZE_BYTES_KEY)
        public String saltSizeBytes;

        @MCRProperty(name = ITERATIONS_KEY)
        public String iterations;

        @Override
        public MCRSHA256Strategy get() {
            return new MCRSHA256Strategy(Integer.parseInt(saltSizeBytes), Integer.parseInt(iterations));
        }

    }

}
