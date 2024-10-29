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

package org.mycore.common;

import java.security.SecureRandom;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.UnixCrypt;

/**
 * Java-based implementation of traditional DES-based password hashing scheme
 * of the POSIX C crypt(3) command.
 */
public class MCRCrypt {

    public static final String ALPHABET = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final Pattern SALT_PATTERN = Pattern.compile("[./a-zA-Z0-9]{2}");

    /**
     * This method hashes a string given as a cleartext string and a salt.
     *
     * @param salt     A two-character string representing the salt used to iterate
     *                 the encryption engine.
     * @param original The string to be hashed.
     * @return A string consisting of the 2-character salt followed by the
     * encrypted string.
     */
    public static String crypt(String salt, String original) {
        if (!SALT_PATTERN.matcher(salt).matches()) {
            throw new IllegalArgumentException("Invalid salt, got " + salt +
                ", expected 2 characters (alphanumerical, '.' or '/')");
        }
        return UnixCrypt.crypt(original, salt);
    }

    /**
     * This method hashes a string given as a cleartext string. A random salt
     * is generated.
     *
     * @param random   The random number generator to be used.
     * @param original The string to be hashed.
     * @return A string consisting of the 2-character salt followed by the
     * encrypted string.
     */
    public static String crypt(SecureRandom random, String original) {
        return crypt(getSalt(random), original);
    }

    /**
     * This method hashes a string given as a cleartext string. A random salt
     * is generated.
     *
     * @param original The string to be hashed.
     * @return A string consisting of the 2-character salt followed by the
     * encrypted string.
     */
    public static String crypt(String original) {
        return crypt(getSalt(new SecureRandom()), original);
    }

    /**
     * Generates a random salt using the provided random number generator.
     *
     * @param random The random number generator to be used.
     * @return A 2-character salt.
     */
    public static String getSalt(SecureRandom random) {
        return new String(new char[]{getRandomCharacter(random), getRandomCharacter(random)});
    }

    private static char getRandomCharacter(SecureRandom random) {
        return ALPHABET.charAt(random.nextInt(ALPHABET.length()));
    }

}
