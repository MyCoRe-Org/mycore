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

package org.mycore.user2.hash.bouncycastle;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.crypto.generators.BCrypt;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.user2.hash.MCRPasswordCheckStrategy;
import org.mycore.user2.hash.MCRPasswordCheckStrategyBase;

import com.google.common.primitives.Chars;

import static org.mycore.user2.hash.MCRPasswordCheckUtils.fixedEffortEquals;

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

    private static final String DELIMITER_REGEX = "\\$";
    private static final String VERSION_REGEX = "2a";
    private static final String COST_REGEX = "([0-9]{2})";
    private static final String SALT_REGEX = "([./a-zA-Z0-9]{22})";
    private static final String HASH_REGEX = "([./a-zA-Z0-9]{31})";

    private static final Pattern BCRYPT_MCF_PATTERN = Pattern.compile(DELIMITER_REGEX + VERSION_REGEX +
        DELIMITER_REGEX + COST_REGEX + DELIMITER_REGEX + SALT_REGEX + HASH_REGEX);

    private static final char[] BASE_64_TO_RADIX_64_MAP;

    private static final char[] RADIX_64_TO_BASE_64_MAP;

    static {

        char[] base64Alphabet = new char[]{
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '+', '/',
            '='
        };

        char[] radix64Alphabet = new char[]{
            '.', '/',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '='
        };

        BASE_64_TO_RADIX_64_MAP = new char[Chars.max(base64Alphabet) + 1];
        for (int i = 0, n = base64Alphabet.length; i < n; i++) {
            BASE_64_TO_RADIX_64_MAP[base64Alphabet[i]] = radix64Alphabet[i];
        }

        RADIX_64_TO_BASE_64_MAP = new char[Chars.max(radix64Alphabet) + 1];
        for (int i = 0, n = radix64Alphabet.length; i < n; i++) {
            RADIX_64_TO_BASE_64_MAP[radix64Alphabet[i]] = base64Alphabet[i];
        }

    }

    private final int cost;

    public MCRBCryptStrategy(int cost) {
        this.cost = cost;
    }

    @Override
    public String invariableConfigurationString() {
        return "";
    }

    @Override
    protected PasswordCheckData doCreate(SecureRandom random, String password) {

        byte[] salt = random.generateSeed(16);
        byte[] hash = getHash(cost, salt, password);

        return new PasswordCheckData(null, compileBCryptMCFString(salt, hash));

    }

    private String compileBCryptMCFString(byte[] salt, byte[] hash) {

        Base64.Encoder encoder = Base64.getEncoder();
        String saltString = convertBase64ToRadix64(encoder.encodeToString(salt));
        String hashString = convertBase64ToRadix64(encoder.encodeToString(hash));
        assert saltString.length() == 24 && hashString.length() == 32;

        return "$2a$" + String.format("%02d", cost) + "$" + saltString.substring(0, 22) + hashString.substring(0, 31);

    }

    @Override
    protected PasswordCheckResult<Boolean> doVerify(PasswordCheckData data, String password) {

        Details details = parseBCryptMCFString(data.hash());

        byte[] hash = getHash(details.cost(), details.salt(), password);
        boolean verified = fixedEffortEquals(details.hash(), hash);
        boolean deprecated = details.cost() != this.cost;

        return new PasswordCheckResult<>(verified, deprecated);

    }

    private Details parseBCryptMCFString(String sting) {

        Matcher matcher = BCRYPT_MCF_PATTERN.matcher(sting);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported input: " + sting);
        }

        Base64.Decoder decoder = Base64.getDecoder();
        int cost = Integer.parseInt(matcher.group(1));
        byte[] salt = decoder.decode(convertRadix64ToBase64(matcher.group(2)));
        byte[] hash = decoder.decode(convertRadix64ToBase64(matcher.group(3)));

        return new Details(cost, salt, hash);

    }

    private byte[] getHash(int cost, byte[] salt, String password) {

        byte[] bytes = (password + '\0').getBytes(StandardCharsets.UTF_8);
        byte[] fullHash = BCrypt.generate(bytes, salt, cost);
        assert fullHash.length == 24;

        byte[] hash = new byte[23];
        System.arraycopy(fullHash, 0, hash, 0, 23);

        return hash;

    }

    private String convertBase64ToRadix64(String string) {
        return convert(BASE_64_TO_RADIX_64_MAP, string);
    }

    private String convertRadix64ToBase64(String string) {
        return convert(RADIX_64_TO_BASE_64_MAP, string);
    }

    private String convert(char[] map, String string) {
        StringBuilder builder = new StringBuilder(string.length());
        char[] charArray = string.toCharArray();
        for (char c : charArray) {
            builder.append(map[c]);
        }
        return builder.toString();
    }

    private record Details(int cost, byte[] salt, byte[] hash) {
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
