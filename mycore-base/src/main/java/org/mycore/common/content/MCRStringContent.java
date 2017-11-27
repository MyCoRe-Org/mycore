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
import java.io.UnsupportedEncodingException;

import org.mycore.common.MCRConstants;

/**
 * Reads MCRContent from a String's text.
 * 
 * @author Frank L\u00FCtzenkichen
 */
public class MCRStringContent extends MCRContent {

    private String text;

    private byte[] bytes;

    /**
     * Reads content from the given string, 
     */
    public MCRStringContent(String text) {
        this.text = text;
        try {
            setEncoding(MCRConstants.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /** 
     * Sets the character encoding to use when transforming the text to a byte stream.
     * By default, this is {@link MCRConstants#DEFAULT_ENCODING}.
     */
    @Override
    public void setEncoding(String encoding) throws UnsupportedEncodingException {
        super.setEncoding(encoding);
        this.bytes = asByteArray();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(asByteArray());
    }

    @Override
    public byte[] asByteArray() throws UnsupportedEncodingException {
        return text.getBytes(encoding);
    }

    public String asString() {
        return text;
    }

    @Override
    public long length() throws IOException {
        return bytes.length;
    }

    @Override
    public long lastModified() throws IOException {
        return -1;
    }

    @Override
    public String getETag() throws IOException {
        String eTag = getSimpleWeakETag(getSystemId(), length(), lastModified());
        return eTag == null ? null : eTag.substring(2);
    }
}
