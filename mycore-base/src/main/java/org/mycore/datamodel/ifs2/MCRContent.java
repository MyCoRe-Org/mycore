/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.datamodel.ifs2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;

import javax.xml.transform.Source;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.VFS;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.mycore.common.MCRException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * Used to read/write content from any source to any target. Sources and targets
 * can be strings, local files, Apache VFS file objects, XML documents, byte[]
 * arrays and streams. MCRContent can only be consumed once, otherwise the
 * getters throw MCRUsageException. Use makeCopies() to avoid this. The
 * underlying input stream is closed after consumption, excepted you use
 * getInputStream(), of course.
 * 
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 */
public abstract class MCRContent {
    /**
     * The content itself
     */
    protected InputStream in;

    /**
     * Holds the systemID of the current content
     */
    protected String systemId;

    protected MCRContent(final String systemId) {
        this.systemId = systemId;
    }

    /**
     * Creates content from byte[] arrray
     * 
     * @param bytes
     *            the content's bytes
     */
    public static MCRContent readFrom(final byte[] bytes) throws IOException {
        return readFrom(new ByteArrayInputStream(bytes));
    }

    /**
     * Creates content from byte[] arrray
     * 
     * @param bytes
     *            the content's bytes
     */
    public static MCRContent readFrom(final byte[] bytes, final String systemId) throws IOException {
        return readFrom(new ByteArrayInputStream(bytes), systemId);
    }

    /**
     * Creates content from XML document. By default, content will be 
     * written pretty-formatted, using UTF-8 encoding and line indentation 
     * depending on the property MCR.IFS2.PrettyXML=true
     * 
     * @param jdom
     *            the XML document to read in as content
     */
    public static MCRContent readFrom(final Document jdom) throws IOException {
        return new MCRContentJDOM(jdom);
    }

    /**
     * Creates content from a local file
     * 
     * @param file
     *            the local file to read
     */
    public static MCRContent readFrom(final File file) throws IOException {
        return readFrom(new FileInputStream(file), file.toURI().toString());
    }

    /**
     * Creates content from Apache VFS file object
     * 
     * @param fo
     *            the file object to read content from
     */
    public static MCRContent readFrom(final FileObject fo) throws IOException {
        return readFrom(fo.getContent().getInputStream(), fo.getURL().toString());
    }

    /**
     * Creates new content from input stream
     * 
     * @param in
     *            the input stream to read content from
     */
    public static MCRContent readFrom(final InputStream in) {
        return new MCRContentIS(in, null);
    }

    /**
     * Creates new content from input stream
     * 
     * @param in
     *            the input stream to read content from
     */
    public static MCRContent readFrom(final InputStream in, final String systemId) {
        return new MCRContentIS(in, systemId);
    }

    public static MCRContent readFrom(final org.w3c.dom.Document dom) throws IOException {
        return new MCRContentDOM(dom);
    }

    /**
     * Creates content from a String, using UTF-8 encoding
     * 
     * @param text
     *            the content
     */
    public static MCRContent readFrom(final String text) throws IOException, UnsupportedEncodingException {
        return readFrom(text, "UTF-8");
    }

    /**
     * Creates content from a String, using the given encoding
     * 
     * @param text
     *            the content
     * @param encoding
     *            the encoding to be used to write bytes
     */
    public static MCRContent readFrom(final String text, final String encoding) throws IOException, UnsupportedEncodingException {
        return readFrom(text.getBytes(encoding));
    }

    /**
     * Creates new content reading from the given URI.
     * 
     * @param uri
     *            the uri to read content from
     */
    public static MCRContent readFrom(final URI uri) throws IOException {
        return readFrom(uri.toURL());
    }

    /**
     * Creates new content reading from the given URL.
     * 
     * @param url
     *            the url to read content from
     */
    public static MCRContent readFrom(final URL url) throws IOException {
        final MCRContent content = readFrom(VFS.getManager().resolveFile(url.toExternalForm()));
        content.systemId = url.toString();
        return content;
    }

    /**
     * Returns the raw content
     * 
     * @return the content
     */
    public byte[] asByteArray() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        sendTo(baos);
        baos.close();
        return baos.toByteArray();
    }

    /**
     * Returns content as String, assuming UTF-8 encoding
     * 
     * @return content as String
     */
    public String asString() throws IOException, UnsupportedEncodingException {
        return asString("UTF-8");
    }

    /**
     * Returns the content as String, assuming the provided encoding
     * 
     * @param encoding
     *            the encoding to use to build the characters
     * @return content as String
     */
    public String asString(final String encoding) throws IOException, UnsupportedEncodingException {
        return new String(asByteArray(), encoding);
    }

    /**
     * Parses content, assuming it is XML, and returns the parsed document.
     * 
     * @return the XML document parsed from content
     * @throws SAXParseException 
     * @throws MCRException 
     */
    public abstract Document asXML() throws JDOMException, IOException, SAXParseException;

    /**
     * Ensures that content is XML
     * @throws SAXParseException 
     */
    public MCRContent ensureXML() throws IOException, JDOMException, SAXParseException {
        MCRContent content = MCRContent.readFrom(asXML());
        content.systemId = systemId;
        return content;
    }

    /**
     * Returns content as content input stream, which provides MD5
     * functionality. Be sure to close this stream properly!
     * 
     * @return the content input stream
     * @throws IOException 
     */
    public MCRContentInputStream getContentInputStream() throws IOException {
        in = getInputStream();
        if (!(in instanceof MCRContentInputStream)) {
            in = new MCRContentInputStream(in);
        }
        return (MCRContentInputStream) in;
    }

    /**
     * Return the document type of the content
     * @return document type as String
     */
    public abstract String getDocType();

    /**
     * Returns content as SAX input source.
     * 
     * @return input source to read content from
     * @throws IOException 
     */
    public InputSource getInputSource() throws IOException {
        final InputSource source = new InputSource(getInputStream());
        source.setSystemId(systemId);
        return source;
    }

    /**
     * Returns content as input stream. Be sure to close this stream properly!
     * 
     * @return input stream to read content from
     * @throws IOException 
     */
    public abstract InputStream getInputStream() throws IOException;

    /**
     * Return the content as Source
     * @return content as Source
     */
    public abstract Source getSource();

    public String getSystemId() {
        return systemId;
    }

    /**
     * Makes copies of the content, consuming this content
     * 
     * @param numCopies
     *            the number of copies to make
     * @return copies of the content
     */
    public abstract MCRContent[] makeCopies(int numCopies) throws IOException;

    /**
     * Sends content to the given local file
     * 
     * @param target
     *            the file to write the content to
     */
    public void sendTo(final File target) throws IOException {
        final OutputStream out = new FileOutputStream(target);
        sendTo(out);
        out.close();
    }

    /**
     * Sends the content to the given file object
     * 
     * @param target
     *            the file to write the content to
     */
    public void sendTo(final FileObject target) throws IOException {
        final OutputStream out = target.getContent().getOutputStream();
        sendTo(out);
        out.close();
    }

    /**
     * Sends content to the given OutputStream
     * 
     * @param out
     *            the OutputStream to write the content to
     */
    public abstract void sendTo(OutputStream out) throws IOException;
}
