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
import java.security.MessageDigest;

import org.jdom2.output.Format;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.streams.MCRMD5InputStream;

/**
 * Reads MCRContent from an XML document.
 * Provides functionality to output XML using different formatters. 
 * 
 * @author Frank L\u00FCtzenkichen
 */
public abstract class MCRXMLContent extends MCRContent {

    /** 
     * The default format used when outputting XML as a byte stream.
     * By default, content is outputted using {@link MCRConstants#DEFAULT_ENCODING}.
     * If MCR.IFS2.PrettyXML=true, a pretty format with indentation is used. 
     */
    protected static Format defaultFormat;

    static {
        boolean prettyXML = MCRConfiguration.instance().getBoolean("MCR.IFS2.PrettyXML", true);
        defaultFormat = prettyXML ? Format.getPrettyFormat().setIndent("  ") : Format.getRawFormat();
        defaultFormat.setEncoding(MCRConstants.DEFAULT_ENCODING);
    }

    /** The default format used when outputting this XML as a byte stream */
    protected Format format = defaultFormat;

    public MCRXMLContent() {
        try {
            this.setEncoding(MCRConstants.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /** 
     * Sets the format used when outputting XML as a byte stream. 
     * By default, content is outputted using {@link MCRConstants#DEFAULT_ENCODING}.
     * If MCR.IFS2.PrettyXML=true, a pretty format with indentation is used. 
     */
    public void setFormat(Format format) {
        this.format = format;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(asByteArray());
    }

    @Override
    public MCRContent ensureXML() {
        return this;
    }

    @Override
    public String getMimeType() throws IOException {
        return super.getMimeType() == null ? "text/xml" : super.getMimeType();
    }

    @Override
    public long length() throws IOException {
        return asByteArray().length;
    }

    @Override
    public String getETag() throws IOException {
        MessageDigest md5Digest = MCRMD5InputStream.buildMD5Digest();
        byte[] byteArray = asByteArray();
        md5Digest.update(byteArray, 0, byteArray.length);
        byte[] digest = md5Digest.digest();
        String md5String = MCRMD5InputStream.getMD5String(digest);
        return '"' + md5String + '"';
    }

    @Override
    public void setEncoding(String encoding) throws UnsupportedEncodingException {
        super.setEncoding(encoding);
        this.format = format.clone();
        format.setEncoding(encoding);
    }
}
