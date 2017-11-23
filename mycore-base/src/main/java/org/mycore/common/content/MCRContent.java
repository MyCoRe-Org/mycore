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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import javax.xml.transform.Source;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.logging.log4j.LogManager;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.common.xsl.MCRLazyStreamSource;
import org.mycore.datamodel.common.MCRDataURL;
import org.mycore.datamodel.common.MCRDataURLEncoding;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Used to read/write content from any source to any target. Sources and targets can be strings, local files, Apache VFS
 * file objects, XML documents, byte[] arrays and streams. The different sources are implemented by subclasses.
 * 
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 */
public abstract class MCRContent {

    /**
     * Holds the systemID of the current content
     */
    protected String systemId;

    /**
     * Holds the docType of the current content
     */
    protected String docType;

    /**
     * Size of content in bytes
     */
    protected long length = -1;

    /**
     * Last modified timestamp
     */
    protected long lastModified = -1;

    protected String mimeType, encoding, name;

    protected boolean usingSession = false;

    /**
     * Sets the systemID of the current content
     */
    void setSystemId(String systemId) {
        this.systemId = systemId;
        if (getName() == null && systemId != null) {
            String fileName = getFilenameFromSystemId();
            setName(fileName);
        }
    }

    private String getFilenameFromSystemId() {
        String fileName = systemId;
        String path = null;
        try {
            path = new URL(systemId).getPath();
        } catch (MalformedURLException e) {
            LogManager.getLogger(getClass()).debug("Could not get file name from URL.", e);
            try {
                path = new URI(systemId).getPath();
            } catch (URISyntaxException e2) {
                LogManager.getLogger(getClass()).debug("Could not get file name from URI.", e2);
            }
        }
        if (path != null) {
            fileName = path;
        }
        if (fileName.endsWith("/")) {
            fileName = FilenameUtils.getPathNoEndSeparator(fileName); //removes final '/';
        }
        return FilenameUtils.getName(fileName);
    }

    /**
     * Returns the systemID of the current content
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Returns content as input stream. Be sure to close this stream properly!
     * 
     * @return input stream to read content from
     */
    public abstract InputStream getInputStream() throws IOException;

    /**
     * Returns an readable bytechannel to this content or null if one is not available.
     */
    public ReadableByteChannel getReadableByteChannel() throws IOException {
        InputStream inputStream = getInputStream();
        return inputStream == null ? null : Channels.newChannel(inputStream);
    }

    /**
     * Returns content as content input stream, which provides MD5 functionality. Be sure to close this stream properly!
     * 
     * @return the content input stream
     */
    public MCRContentInputStream getContentInputStream() throws IOException {
        return new MCRContentInputStream(getInputStream());
    }

    /**
     * Return the content as Source
     * 
     * @return content as Source
     */
    public Source getSource() throws IOException {
        return new MCRLazyStreamSource(this::getInputStream, getSystemId());
    }

    /**
     * Sends content to the given OutputStream. The OutputStream is NOT automatically closed afterwards.
     * 
     * @param out
     *            the OutputStream to write the content to
     */
    public void sendTo(OutputStream out) throws IOException {
        try (InputStream in = getInputStream()) {
            IOUtils.copy(in, out);
        }
    }

    /**
     * Sends content to the given OutputStream.
     * 
     * @param out
     *            the OutputStream to write the content to
     * @param close
     *            if true, close OutputStream afterwards
     */
    public void sendTo(OutputStream out, boolean close) throws IOException {
        try {
            sendTo(out);
        } finally {
            if (close) {
                out.close();
            }
        }
    }

    /**
     * Returns content as SAX input source.
     * 
     * @return input source to read content from
     */
    public InputSource getInputSource() throws IOException {
        InputSource source = new InputSource(getInputStream());
        source.setSystemId(getSystemId());
        return source;
    }

    /**
     * Sends content to the given local file
     * 
     * @param target
     *            the file to write the content to
     */
    public void sendTo(File target) throws IOException {
        sendTo(new FileOutputStream(target), true);
    }

    /**
     * Sends content to the given path.
     * @param target target path to write content to
     * @param options see {@link Files#copy(InputStream, Path, CopyOption...)}} for help on copy options
     */
    public void sendTo(Path target, CopyOption... options) throws IOException {
        try (InputStream in = getInputStream()) {
            Files.copy(in, target, options);
        }
    }

    /**
     * Sends the content to the given Apache VFS file object
     * 
     * @param target
     *            the file to write the content to
     */
    public void sendTo(FileObject target) throws IOException {
        sendTo(target.getContent().getOutputStream(), true);
    }

    /**
     * Returns the raw content
     * 
     * @return the content
     */
    public byte[] asByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        sendTo(baos);
        baos.close();
        return baos.toByteArray();
    }

    /**
     * Returns content as String, assuming encoding from {@link #getEncoding()} or {@link MCRConstants#DEFAULT_ENCODING}
     * .
     * 
     * @return content as String
     */
    public String asString() throws IOException {
        return new String(asByteArray(), getSafeEncoding());
    }

    /**
     * Returns content as "data:" URL.
     * @throws IOException
     */
    public MCRDataURL asDataURL() throws IOException {
        return new MCRDataURL(asByteArray(), getDataURLEncoding(), getMimeType(), getSafeEncoding());
    }

    protected MCRDataURLEncoding getDataURLEncoding() throws IOException {
        return getMimeType().startsWith("text/") ? MCRDataURLEncoding.URL
            : MCRDataURLEncoding.BASE64;
    }

    /**
     * Parses content, assuming it is XML, and returns the parsed document.
     * 
     * @return the XML document parsed from content
     */
    public Document asXML() throws JDOMException, IOException, SAXException {
        return MCRXMLParserFactory.getNonValidatingParser().parseXML(this);
    }

    /**
     * Ensures that content is XML. The content is parsed as if asXML() is called. When content is XML, an MCRContent
     * instance is returned that guarantees that. When XML can not be parsed, an exception is thrown.
     */
    public MCRContent ensureXML() throws IOException, JDOMException, SAXException {
        return new MCRJDOMContent(asXML());
    }

    /**
     * Return the document type of the content, assuming content is XML
     *
     * @return document type as String
     */
    public String getDocType() throws IOException {
        if (docType != null) {
            return docType;
        }
        if (!isReusable()) {
            throw new IOException("Cannot determine DOCTYPE as it would destroy underlaying InputStream.");
        }
        try (MCRContentInputStream cin = getContentInputStream()) {
            byte[] header = cin.getHeader();
            return MCRUtils.parseDocumentType(new ByteArrayInputStream(header));
        }
    }

    /**
     * Overwrites DocType detection.
     * 
     * @see MCRContent#getDocType()
     */
    public void setDocType(String docType) {
        this.docType = docType;
    }

    /**
     * If true, content can be read more than once by calling getInputStream() and similar methods. If false, content
     * may be consumed when it is read more than once. Most subclasses provide reusable content.
     */
    public boolean isReusable() {
        return true;
    }

    /**
     * Returns a reusable copy of this content, that is an instance (may be the same instance) thats content can be read
     * more than once without consuming the stream.
     */
    public MCRContent getReusableCopy() throws IOException {
        if (isReusable()) {
            return this;
        } else {
            MCRContent copy = new MCRByteContent(asByteArray(), lastModified());
            copy.setSystemId(getSystemId());
            copy.setName(getName());
            copy.setDocType(getDocType());
            copy.setMimeType(getMimeType());
            return copy;
        }
    }

    /**
     * Return the length of this content.
     * 
     * @return -1 if length is unknown
     */
    public long length() throws IOException {
        return length;
    }

    /**
     * Returns the last modified time
     * 
     * @return -1 if last modified time is unknown
     */
    public long lastModified() throws IOException {
        return lastModified;
    }

    /**
     * Returns either strong or weak ETag.
     * 
     * @return null, if no ETag could be generated
     */
    public String getETag() throws IOException {
        return getSimpleWeakETag(getSystemId(), length, lastModified);
    }

    /**
     * Uses provided parameter to compute simple weak ETag.
     * 
     * @param systemId
     *            != null, {@link #getSystemId()}
     * @param length
     *            &gt;= 0, {@link #length()}
     * @param lastModified
     *            &gt;= 0, {@link #lastModified()}
     * @return null if any preconditions are not met.
     */
    protected String getSimpleWeakETag(String systemId, long length, long lastModified) {
        if (systemId == null || length < 0 || lastModified < 0) {
            return null;
        }
        StringBuilder b = new StringBuilder(32);
        b.append("W/\"");
        long lhash = systemId.hashCode();
        byte[] unencodedETag = ByteBuffer.allocate(Long.SIZE / 4).putLong(lastModified ^ lhash).putLong(length ^ lhash)
            .array();
        b.append(Base64.getEncoder().encodeToString(unencodedETag));
        b.append('"');
        return b.toString();
    }

    public String getMimeType() throws IOException {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Tells if this content may contain data from the current MCRSession. Use this information to alter cache behavior.
     * 
     * @return true if it MAY contain session data
     */
    public boolean isUsingSession() {
        return usingSession;
    }

    public void setUsingSession(boolean usingSession) {
        this.usingSession = usingSession;
    }

    public String getEncoding() {
        return encoding;
    }

    protected String getSafeEncoding() {
        String enc = getEncoding();
        return enc != null ? enc : MCRConstants.DEFAULT_ENCODING;
    }

    public void setEncoding(String encoding) throws UnsupportedEncodingException {
        this.encoding = encoding;
    }
}
