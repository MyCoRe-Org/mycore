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

package org.mycore.user2.hash.favre;

import java.security.SecureRandom;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.user2.hash.MCRPasswordCheckStrategy;
import org.mycore.user2.hash.MCRPasswordCheckStrategyBase;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategy;

/**
 * {@link MCRBCryptStrategy} is n implementation of {@link MCRPasswordCheckStrategy} that
 * uses the BCrypt algorithm.
 * <p>
 * Version, cost and salt are encoded in the hash using the Modular Crypt Format (MCF).
 * <p>
 * The verification result will be marked as outdated if the cost doesn't equal the expected value.
 */
@MCRConfigurationProxy(proxyClass = MCRBCryptStrategy.Factory.class)
public class MCRBCryptStrategy extends MCRPasswordCheckStrategyBase {

    public static final BCrypt.Version VERSION = BCrypt.Version.VERSION_2A;

    public static final LongPasswordStrategy LONG_PASSWORD_STRATEGY = LongPasswordStrategies.strict(VERSION);

    private final int cost;

    public MCRBCryptStrategy(int cost) {
        this.cost = cost;
    }

    @Override
    public String invariableConfiguration() {
        return "";
    }

    @Override
    protected PasswordCheckData doCreate(SecureRandom random, String password) {
        String hash = BCrypt.with(VERSION, random, LONG_PASSWORD_STRATEGY)
            .hashToString(cost, password.toCharArray());
        return new PasswordCheckData(null, hash);
    }

    @Override
    protected PasswordCheckResult<Boolean> doVerify(PasswordCheckData data, String password) {
        BCrypt.Result result = BCrypt.verifyer(VERSION, LONG_PASSWORD_STRATEGY)
            .verify(password.toCharArray(), data.hash().toCharArray());
        boolean deprecated = result.details.cost != cost;
        return new PasswordCheckResult<>(result.verified, deprecated);
    }

    public static class Factory implements Supplier<MCRBCryptStrategy> {

        @MCRProperty(name = "Cost")
        public String cost;

        @Override
        public MCRBCryptStrategy get() {
            return new MCRBCryptStrategy(Integer.parseInt(cost));
        }

    }

}
