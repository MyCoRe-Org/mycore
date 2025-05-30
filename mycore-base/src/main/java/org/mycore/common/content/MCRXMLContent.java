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
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import org.jdom2.output.Format;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.digest.MCRMD5Digest;

/**
 * Reads MCRContent from an XML document.
 * Provides functionality to output XML using different formatters. 
 * 
 * @author Frank Lützenkichen
 */
public abstract class MCRXMLContent extends MCRContent {

    /** 
     * The default format used when outputting XML as a byte stream.
     * By default, content is outputted using {@link MCRConstants#DEFAULT_ENCODING}.
     * If MCR.IFS2.PrettyXML=true, a pretty format with indentation is used. 
     */
    private static final Format DEFAULT_FORMAT;

    static {
        boolean prettyXML = MCRConfiguration2.getBoolean("MCR.IFS2.PrettyXML").orElse(true);
        DEFAULT_FORMAT = prettyXML ? Format.getPrettyFormat().setIndent("  ") : Format.getRawFormat();
        DEFAULT_FORMAT.setEncoding(MCRConstants.DEFAULT_ENCODING);
    }

    /** The default format used when outputting this XML as a byte stream */
    protected Format format = DEFAULT_FORMAT;

    public MCRXMLContent() {
        try {
            this.setEncoding(MCRConstants.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new MCRException("MCRXMLContent encoding not supported", e);
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
        MessageDigest md5Digest = MCRUtils.buildMessageDigest(MCRMD5Digest.ALGORITHM);
        byte[] byteArray = asByteArray();
        md5Digest.update(byteArray, 0, byteArray.length);
        byte[] digest = md5Digest.digest();
        String md5String = MCRUtils.toHexString(digest);
        return '"' + md5String + '"';
    }

    @Override
    public void setEncoding(String encoding) throws UnsupportedEncodingException {
        super.setEncoding(encoding);
        this.format = format.clone();
        format.setEncoding(encoding);
    }
}
