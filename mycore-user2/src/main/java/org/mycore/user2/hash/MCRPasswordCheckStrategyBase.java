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

import org.mycore.common.MCRException;

/**
 * {@link MCRPasswordCheckStrategyBase} is a base implementation of {@link MCRPasswordCheckStrategy} that
 * facilitates consistent exception handling.
 */
public abstract class MCRPasswordCheckStrategyBase implements MCRPasswordCheckStrategy {

    @Override
    public final MCRPasswordCheckData create(SecureRandom random, String type, String password) {
        try {
            PasswordCheckData data = doCreate(random, password);
            return new MCRPasswordCheckData(type, data.salt(), data.hash());
        } catch (Exception e) {
            throw new MCRException("Failed to create password hash", e);
        }
    }

    protected abstract PasswordCheckData doCreate(SecureRandom random, String password) throws Exception;

    @Override
    public final MCRPasswordCheckResult verify(MCRPasswordCheckData data, String password) {
        try {
            PasswordCheckResult<Boolean> result = doVerify(new PasswordCheckData(data.salt(), data.hash()), password);
            return new MCRPasswordCheckResult(result.value(), result.deprecated());
        } catch (Exception e) {
            throw new MCRException("Failed to recreate password hash", e);
        }
    }

    protected abstract PasswordCheckResult<Boolean> doVerify(PasswordCheckData data, String password) throws Exception;

    public record PasswordCheckData(String salt, String hash) {
    }

    public record PasswordCheckResult<T>(T value, boolean deprecated) {
    }

}
