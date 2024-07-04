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
import java.util.function.Supplier;

import org.mycore.common.MCRUtils;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * {@link MCRMD5Strategy} is n implementation of {@link MCRPasswordCheckStrategy} that
 * uses the MD5 algorithm.
 */
@MCRConfigurationProxy(proxyClass = MCRMD5Strategy.Factory.class)
public class MCRMD5Strategy extends MCRHashPasswordCheckStrategy {

    private final int iterations;

    public MCRMD5Strategy(int iterations) {
        this.iterations = iterations;
    }

    @Override
    protected final PasswordCheckData doCreate(SecureRandom random, String password) throws Exception {
        return new PasswordCheckData(null, MCRUtils.asMD5String(iterations, null, password));
    }

    @Override
    protected final PasswordCheckResult<String> doRecreate(PasswordCheckData data, String password) throws Exception {
        return new PasswordCheckResult<>(MCRUtils.asMD5String(iterations, null, password), false);
    }

    public static class Factory implements Supplier<MCRMD5Strategy> {

        @MCRProperty(name = "Iterations")
        public String iterations;

        @Override
        public MCRMD5Strategy get() {
            return new MCRMD5Strategy(Integer.parseInt(iterations));
        }

    }

}
