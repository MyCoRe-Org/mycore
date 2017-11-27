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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;

import org.mycore.common.content.streams.MCRMD5InputStream;

/**
 * Reads MCRContent from a byte[] array.
 * 
 * @author Frank L\u00FCtzenkichen
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
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(bytes, offset, length);
    }

    @Override
    public byte[] asByteArray() throws IOException {
        if (offset == 0 && length == 0) {
            return bytes;
        }
        synchronized (this) {
            if (offset == 0 && length == bytes.length) {
                return bytes;
            }
            bytes = Arrays.copyOfRange(bytes, offset, offset + length);
            offset = 0;
            length = bytes.length;
        }
        return bytes;
    }

    @Override
    public long length() throws IOException {
        return length;
    }

    @Override
    public long lastModified() throws IOException {
        return lastModified;
    }

    @Override
    public String getETag() throws IOException {
        MessageDigest md5Digest = MCRMD5InputStream.buildMD5Digest();
        md5Digest.update(bytes, offset, length);
        byte[] digest = md5Digest.digest();
        String md5String = MCRMD5InputStream.getMD5String(digest);
        return '"' + md5String + '"';
    }
}
