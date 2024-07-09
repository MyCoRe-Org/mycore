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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

import com.google.common.primitives.Bytes;

import static org.mycore.user2.hash.MCRPasswordCheckUtils.fixedEffortEquals;

/**
 * {@link MCRS2KStrategy} is n implementation of {@link MCRPasswordCheckStrategy} that
 * uses the iterated and salted String-to-Key (S2K) algorithm describes in RFC 4880 (OpenPGP Message Format).
 * <p>
 * The salt and the hash are returned as hex encoded strings.
 * <p>
 * The verification result will be marked as outdated if the salt size or the hash size doesn't equal the
 * expected value.
 * <p>
 * Changes to the count will result in deviating hashes and therefore prevent the successful
 * verification of existing hashes, even if the correct password is supplied.
 */
@MCRConfigurationProxy(proxyClass = MCRS2KStrategy.Factory.class)
public class MCRS2KStrategy extends MCRPasswordCheckStrategyBase {

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private final int saltSizeBytes;

    private final int hashSizeBytes;

    private final int count;

    public MCRS2KStrategy(int saltSizeBytes, int hashSizeBytes, int count) {
        this.saltSizeBytes = saltSizeBytes;
        this.hashSizeBytes = hashSizeBytes;
        this.count = count;
    }

    @Override
    public String invariableConfigurationString() {
        return "c=" + count;
    }

    @Override
    protected PasswordCheckData doCreate(SecureRandom random, String password) throws Exception {

        byte[] salt = random.generateSeed(saltSizeBytes);
        byte[] hash = getHash(salt, hashSizeBytes, password);

        return new PasswordCheckData(HEX_FORMAT.formatHex(salt), HEX_FORMAT.formatHex(hash));

    }

    @Override
    protected PasswordCheckResult<Boolean> doVerify(PasswordCheckData data, String password) throws Exception {

        byte[] checkSalt = HEX_FORMAT.parseHex(data.salt());
        byte[] checkHash = HEX_FORMAT.parseHex(data.hash());
        byte[] hash = getHash(checkSalt, checkHash.length, password);

        boolean verified = fixedEffortEquals(HEX_FORMAT.parseHex(data.hash()), hash);
        boolean deprecated = checkSalt.length != saltSizeBytes || checkHash.length != hashSizeBytes;

        return new PasswordCheckResult<>(verified, deprecated);

    }

    private byte[] getHash(byte[] salt, int hashSizeBytes, String password) throws Exception {

        long numberOfBytes = ((long) (16 + (count & 15)) << ((count >> 4) + 6));
        byte[] source = Bytes.concat(salt, password.getBytes(StandardCharsets.UTF_8));

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        int digestLength = digest.getDigestLength();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 0, n = hashSizeBytes / digestLength; i < n; i++) {
            buffer.write(getHash(digest, source, numberOfBytes, i));
            digest.reset();
        }
        buffer.write(getHash(digest, source, numberOfBytes, hashSizeBytes / digestLength),
            0, hashSizeBytes - (hashSizeBytes / digestLength) * digestLength);

        return buffer.toByteArray();

    }

    private byte[] getHash(MessageDigest digest, byte[] source, long numberOfBytes, int preloadCount) {

        for (int i = 0; i < preloadCount; i++) {
            digest.update((byte) 0);
        }

        for (long i = 0, n = numberOfBytes / source.length; i < n; i++) {
            digest.update(source);
        }
        digest.update(source, 0, (int) (numberOfBytes - (numberOfBytes / source.length) * source.length));

        return digest.digest();

    }

    public static class Factory implements Supplier<MCRS2KStrategy> {

        @MCRProperty(name = "SaltSizeBytes")
        public String saltSizeBytes;

        @MCRProperty(name = "HashSizeBytes")
        public String hashSizeBytes;

        @MCRProperty(name = "Count")
        public String count;

        @Override
        public MCRS2KStrategy get() {
            return new MCRS2KStrategy(Integer.parseInt(saltSizeBytes), Integer.parseInt(hashSizeBytes),
                Integer.parseInt(count));
        }

    }

}
