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
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * {@link MCRSHA1Strategy} is n implementation of {@link MCRPasswordCheckStrategy} that
 * uses the SHA1 algorithm.
 * <p>
 * The salt is stored as a Base64 encoded String. The verification result will be marked as outdated if the size of
 * the salt doesn't equal the expected size.
 */
@MCRConfigurationProxy(proxyClass = MCRSHA1Strategy.Factory.class)
public class MCRSHA1Strategy extends MCRSaltedHashPasswordCheckStrategy {

    private final int iterations;

    public MCRSHA1Strategy(int saltSizeBytes, int iterations) {
        super(saltSizeBytes);
        this.iterations = iterations;
    }

    @Override
    protected String doCreateSaltedHash(byte[] salt, String password) throws Exception {
        return MCRUtils.asSHA1String(iterations, salt, password);
    }

    public static class Factory implements Supplier<MCRSHA1Strategy> {

        @MCRProperty(name = "SaltSizeBytes")
        public String saltSizeBytes;

        @MCRProperty(name = "Iterations")
        public String iterations;

        @Override
        public MCRSHA1Strategy get() {
            return new MCRSHA1Strategy(Integer.parseInt(saltSizeBytes), Integer.parseInt(iterations));
        }

    }

}
