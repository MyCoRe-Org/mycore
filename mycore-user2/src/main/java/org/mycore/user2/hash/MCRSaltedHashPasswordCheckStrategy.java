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

import java.security.SecureRandom;
import java.util.Base64;

/**
 * A {@link MCRSaltedHashPasswordCheckStrategy} is a base implementation of {@link MCRPasswordCheckStrategy} that
 * facilitates verification by comparing a generated hash string and employs a randomly generated salt.
 * <p>
 * The salt is stored as a Base64 encoded String. The verification result will be marked as outdated if the size of
 * the salt doesn't equal the expected size.
 */
public abstract class MCRSaltedHashPasswordCheckStrategy extends MCRHashPasswordCheckStrategy {

    private final int saltSizeBytes;

    public MCRSaltedHashPasswordCheckStrategy(int saltSizeBytes) {
        this.saltSizeBytes = saltSizeBytes;
        if (saltSizeBytes < 0) {
            throw new IllegalArgumentException("Salt size [bytes] must be non-negative, got " + saltSizeBytes);
        }
    }

    @Override
    protected final PasswordCheckData doCreate(SecureRandom random, String password) throws Exception {
        byte[] salt = random.generateSeed(saltSizeBytes);
        String encodedSalt = Base64.getEncoder().encodeToString(salt);
        return new PasswordCheckData(encodedSalt, doCreateSaltedHash(salt, password));
    }

    @Override
    protected final PasswordCheckResult<String> doRecreate(PasswordCheckData data, String password) throws Exception {
        byte[] decodedSalt = Base64.getDecoder().decode(data.salt());
        boolean deprecated = decodedSalt.length != saltSizeBytes;
        return new PasswordCheckResult<>(doCreateSaltedHash(decodedSalt, password), deprecated);
    }

    protected abstract String doCreateSaltedHash(byte[] salt, String password) throws Exception;

}
