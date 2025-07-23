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

package org.mycore.ocfl.niofs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.mycore.common.MCRUtils;
import org.mycore.common.digest.MCRDigest;
import org.mycore.common.digest.MCRSHA512Digest;

/**
 * An implementation of {@link MCROCFLDigestCalculator} that uses SHA-512.
 * <p>
 * This class is designed to be injected by the MCR configuration system.
 */
public class MCROCFLSha512DigestCalculator implements MCROCFLDigestCalculator<Path, MCRDigest> {

    @Override
    public MCRDigest calculate(byte[] bytes) throws IOException {
        String hexDigest = MCRUtils.getDigest(MCRSHA512Digest.ALGORITHM, new ByteArrayInputStream(bytes));
        return new MCRSHA512Digest(hexDigest);
    }

    @Override
    public MCRDigest calculate(Path path) throws IOException {
        String digestValue = MCRUtils.getDigest(MCRSHA512Digest.ALGORITHM, Files.newInputStream(path));
        return new MCRSHA512Digest(digestValue);
    }

}
