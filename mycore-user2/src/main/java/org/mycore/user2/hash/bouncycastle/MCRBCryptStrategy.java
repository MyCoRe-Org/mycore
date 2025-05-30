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

package org.mycore.user2.hash.bouncycastle;

import static com.google.common.primitives.Chars.max;
import static org.mycore.user2.hash.MCRPasswordCheckUtils.fixedEffortEquals;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.crypto.generators.BCrypt;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.user2.hash.MCRPasswordCheckStrategy;
import org.mycore.user2.hash.MCRPasswordCheckStrategyBase;

/**
 * A {@link MCRBCryptStrategy} is a {@link MCRPasswordCheckStrategy} that uses the BCrypt algorithm.
 * <p>
 * The version and salt are encoded in the hash using the Modular Crypt Format (MCF) for BCrypt.
 * No explicit salt values are generated.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRBCryptStrategy#COST_KEY} can be used to
 * specify the cost parameter that determines the number of iterations to be performed.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.user2.hash.bouncycastle.MCRBCryptStrategy
 * [...].Cost=12
 * </code></pre>
 * Changes to the cost parameter will not prevent verification, but successful verification results
 * will be marked as outdated.
 */
@MCRConfigurationProxy(proxyClass = MCRBCryptStrategy.Factory.class)
public class MCRBCryptStrategy extends MCRPasswordCheckStrategyBase {

    private static final Pattern BCRYPT_MCF_PATTERN;

    public static final String COST_KEY = "Cost";

    static {

        String delimiterRegex = "\\$";
        String versionRegex = "2a";
        String costRegex = "([0-9]{2})";
        String saltRegex = "([./a-zA-Z0-9]{22})";
        String hashRegex = "([./a-zA-Z0-9]{31})";

        BCRYPT_MCF_PATTERN = Pattern.compile(delimiterRegex + versionRegex +
            delimiterRegex + costRegex + delimiterRegex + saltRegex + hashRegex);

    }

    private static final char[] BASE_64_TO_RADIX_64_MAP;

    private static final char[] RADIX_64_TO_BASE_64_MAP;

    static {

        char[] base64Alphabet = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '+', '/',
            '=',
        };

        char[] radix64Alphabet = {
            '.', '/',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '=',
        };

        BASE_64_TO_RADIX_64_MAP = new char[max(base64Alphabet) + 1];
        for (int i = 0; i < base64Alphabet.length; i++) {
            BASE_64_TO_RADIX_64_MAP[base64Alphabet[i]] = radix64Alphabet[i];
        }

        RADIX_64_TO_BASE_64_MAP = new char[max(radix64Alphabet) + 1];
        for (int i = 0; i < radix64Alphabet.length; i++) {
            RADIX_64_TO_BASE_64_MAP[radix64Alphabet[i]] = base64Alphabet[i];
        }

    }

    private final int cost;

    public MCRBCryptStrategy(int cost) {
        if (cost < 4 || cost > 31) {
            throw new IllegalArgumentException("Cost must be between 4 and 31 (inclusive), got " + cost);
        }
        this.cost = cost;
    }

    @Override
    public String unmodifiableConfigurationHint() {
        return "";
    }

    @Override
    protected PasswordCheckData doCreate(SecureRandom random, String password) {

        byte[] salt = random.generateSeed(16);
        byte[] hash = getHash(cost, salt, password);

        return new PasswordCheckData("", compileBCryptMCFString(salt, hash));

    }

    private String compileBCryptMCFString(byte[] salt, byte[] hash) {

        Base64.Encoder encoder = Base64.getEncoder();
        String saltString = convertBase64ToRadix64(encoder.encodeToString(salt));
        String hashString = convertBase64ToRadix64(encoder.encodeToString(hash));
        assert saltString.length() == 24 && hashString.length() == 32;

        return "$2a$" + formatTwoDigitNumber(cost) + "$" + saltString.substring(0, 22) + hashString.substring(0, 31);

    }

    private static String formatTwoDigitNumber(int number) {
        return String.format(Locale.ENGLISH, "%02d", number);
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

        @MCRProperty(name = COST_KEY)
        public String cost;

        @Override
        public MCRBCryptStrategy get() {
            return new MCRBCryptStrategy(Integer.parseInt(cost));
        }

    }

}
