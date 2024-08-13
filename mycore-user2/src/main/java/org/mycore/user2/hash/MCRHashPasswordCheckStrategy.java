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

import static org.mycore.user2.hash.MCRPasswordCheckUtils.fixedEffortEquals;

/**
 * A {@link MCRHashPasswordCheckStrategy} is a base implementation of {@link MCRPasswordCheckStrategy} that
 * facilitates verification by comparing a generated hash string.
 */
public abstract class MCRHashPasswordCheckStrategy extends MCRPasswordCheckStrategyBase {

    @Override
    protected final PasswordCheckResult<Boolean> doVerify(PasswordCheckData data, String password) throws Exception {
        PasswordCheckResult<String> result = doRecreate(data, password);
        return new PasswordCheckResult<>(fixedEffortEquals(result.value(), data.hash()), result.deprecated());
    }

    protected abstract PasswordCheckResult<String> doRecreate(PasswordCheckData data, String password) throws Exception;

}
