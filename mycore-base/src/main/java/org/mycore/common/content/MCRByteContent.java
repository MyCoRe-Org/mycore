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

package org.mycore.common.content;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;

import org.mycore.common.MCRUtils;
import org.mycore.common.digest.MCRMD5Digest;

/**
 * Reads MCRContent from a byte[] array.
 *
 * @author Frank Lützenkichen
 */
public class MCRByteContent extends MCRContent {

    private byte[] bytes;

    private int offset;

    private int length;

    private long lastModified;

    public MCRByteContent(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public MCRByteContent(byte[] bytes, int offset, int length) {
        this(bytes, offset, length, System.currentTimeMillis());
    }

    public MCRByteContent(byte[] bytes, long lastModified) {
        this(bytes, 0, bytes.length, lastModified);
    }

    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    public MCRByteContent(byte[] bytes, int offset, int length, long lastModified) {
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
        this.lastModified = lastModified;
    }

    @Override
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(bytes, offset, length);
    }

    @Override
    public byte[] asByteArray() {
        if (offset == 0 && length == 0) {
            return bytes.clone();
        }
        synchronized (this) {
            if (offset == 0 && length == bytes.length) {
                return bytes.clone();
            }
            bytes = Arrays.copyOfRange(bytes, offset, offset + length);
            offset = 0;
            length = bytes.length;
        }
        return bytes.clone();
    }

    @Override
    public long length() {
        return length;
    }

    @Override
    public long lastModified() {
        return lastModified;
    }

    @Override
    public String getETag() {
        MessageDigest md5Digest = MCRUtils.buildMessageDigest(MCRMD5Digest.ALGORITHM);
        md5Digest.update(bytes, offset, length);
        byte[] digest = md5Digest.digest();
        String md5String = MCRUtils.toHexString(digest);
        return '"' + md5String + '"';
    }
}
