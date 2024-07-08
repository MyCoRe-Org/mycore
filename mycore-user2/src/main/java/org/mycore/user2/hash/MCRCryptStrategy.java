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

/**
 * {@link MCRCryptStrategy} is n implementation of {@link MCRPasswordCheckStrategy} that
 * uses the algorithm of the Unix <code>crypt</code> command implemented in {@link org.mycore.common.MCRCrypt}.
 */
@MCRConfigurationProxy(proxyClass = MCRCryptStrategy.Factory.class)
public class MCRCryptStrategy extends MCRHashPasswordCheckStrategy {

    @Override
    public String invariableConfigurationString() {
        return "";
    }

    @Override
    protected final PasswordCheckData doCreate(SecureRandom random, String password) {
        return new PasswordCheckData(null, MCRUtils.asCryptString("", password));
    }

    @Override
    protected final PasswordCheckResult<String> doRecreate(PasswordCheckData data, String password) {
        return new PasswordCheckResult<>(MCRUtils.asCryptString(data.hash().substring(0, 3), password), false);
    }

    public static class Factory implements Supplier<MCRCryptStrategy> {

        @Override
        public MCRCryptStrategy get() {
            return new MCRCryptStrategy();
        }

    }

}
