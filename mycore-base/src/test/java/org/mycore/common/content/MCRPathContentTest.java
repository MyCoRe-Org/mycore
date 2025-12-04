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

package org.mycore.common.content;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.jupiter.api.Test;
import org.mycore.common.digest.MCRDigest;
import org.mycore.common.digest.MCRMD5Digest;
import org.mycore.datamodel.niofs.MCRDefaultFileAttributes;
import org.mycore.datamodel.niofs.MCRFileAttributes;

class MCRPathContentTest {

    @Test
    void getETag() throws IOException {
        Path path = Path.of("src", "test", "resources", "MCRContentTest", "test.xml");
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        assertNotNull(attrs);
        MCRPathContent content = new MCRPathContent(path, attrs);
        String etag = content.getETag();
        assertNotNull(etag);
        assertTrue(MCREtagHelper.isValidEtag(etag), "ETag is not valid: " + etag);
    }

    @Test
    void getDigestETag() throws IOException {
        Path path = Path.of("src", "test", "resources", "MCRContentTest", "test.xml");
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        assertNotNull(attrs);
        MCRDigest digest = new MCRMD5Digest("facedbadc0ffee1234567890deadbeef");
        MCRFileAttributes mcrattrs = MCRDefaultFileAttributes.ofAttributes(attrs, digest);
        MCRPathContent content = new MCRPathContent(path, mcrattrs);
        String etag = content.getETag();
        assertNotNull(etag);
        assertTrue(MCREtagHelper.isValidEtag(etag), "ETag is not valid: " + etag);
    }
}
