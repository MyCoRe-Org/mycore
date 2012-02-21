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

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.VFS;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRXMLParserFactory;
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
 */
public class MCRContent {

    /**
     * The content itself
     */
    protected InputStream in;

    protected Document jdom;

    protected org.w3c.dom.Document dom;

    /**
     * If true, this content already was used and cannot be used again
     */
    protected boolean consumed = false;

    /**
     * If true, we are absolutely sure that source is XML
     */
    protected boolean isXML = false;

    /**
     * Holds the systemID of the current content
     */
    protected String systemId;

    /**
     * The format in which XML content is written. By default, this is 
     * pretty format using indentation and line breaks, UTF-8 encoded.
     * This can be changed to raw format without formatting by setting
     * MCR.IFS2.PrettyXML=false.
     */
    private static Format xmlFormat;

    private MCRContentFormat format;

    static {
        boolean prettyXML = MCRConfiguration.instance().getBoolean("MCR.IFS2.PrettyXML", true);
        xmlFormat = prettyXML ? Format.getPrettyFormat().setIndent("  ") : Format.getRawFormat();
        xmlFormat.setEncoding("UTF-8");
    }

    /**
     * Creates content from a String, using UTF-8 encoding
     * 
     * @param text
     *            the content
     */
    public static MCRContent readFrom(String text) throws IOException, UnsupportedEncodingException {
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
    public static MCRContent readFrom(String text, String encoding) throws IOException, UnsupportedEncodingException {
        return readFrom(text.getBytes(encoding));
    }

    /**
     * Creates content from a local file
     * 
     * @param file
     *            the local file to read
     */
    public static MCRContent readFrom(File file) throws IOException {
        return readFrom(new FileInputStream(file), file.toURI().toString());
    }

    /**
     * Creates content from Apache VFS file object
     * 
     * @param fo
     *            the file object to read content from
     */
    public static MCRContent readFrom(FileObject fo) throws IOException {
        return readFrom(fo.getContent().getInputStream(), fo.getURL().toString());
    }

    /**
     * Creates content from byte[] arrray
     * 
     * @param bytes
     *            the content's bytes
     */
    public static MCRContent readFrom(byte[] bytes) throws IOException {
        return readFrom(new ByteArrayInputStream(bytes));
    }

    /**
     * Creates content from byte[] arrray
     * 
     * @param bytes
     *            the content's bytes
     */
    public static MCRContent readFrom(byte[] bytes, String systemId) throws IOException {
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
    public static MCRContent readFrom(Document jdom) throws IOException {
        return new MCRContent(jdom);
    }

    public static MCRContent readFrom(org.w3c.dom.Document dom) throws IOException {
        return new MCRContent(dom);
    }

    /**
     * Creates new content from input stream
     * 
     * @param in
     *            the input stream to read content from
     */
    public static MCRContent readFrom(InputStream in) {
        return new MCRContent(in, null);
    }

    /**
     * Creates new content from input stream
     * 
     * @param in
     *            the input stream to read content from
     */
    public static MCRContent readFrom(InputStream in, String systemId) {
        return new MCRContent(in, systemId);
    }

    private MCRContent(InputStream in, String systemId) {
        this.in = in;
        this.systemId = systemId;
        this.format = MCRContentFormat.INPUT_STREAM;
    }

    private MCRContent(Document jdom) throws IOException {
        this.jdom = jdom;
        this.format = MCRContentFormat.JDOM;
        this.isXML = true;
        this.systemId = null;
    }

    private MCRContent(org.w3c.dom.Document dom) throws IOException {
        this.dom = dom;
        this.format = MCRContentFormat.DOM;
        this.isXML = true;
        this.systemId = null;
    }

    /**
     * Creates new content reading from the given URL.
     * 
     * @param url
     *            the url to read content from
     */
    public static MCRContent readFrom(URL url) throws IOException {
        MCRContent content = readFrom(VFS.getManager().resolveFile(url.toExternalForm()));
        content.systemId = url.toString();
        return content;
    }

    /**
     * Creates new content reading from the given URI.
     * 
     * @param uri
     *            the uri to read content from
     */
    public static MCRContent readFrom(URI uri) throws IOException {
        return readFrom(uri.toURL());
    }

    /**
     * Ensures that content is XML
     * @throws SAXParseException 
     */
    public MCRContent ensureXML() throws IOException, JDOMException, SAXParseException {
        if (isXML) {
            return this;
        } else {
            return MCRContent.readFrom(asXML());
        }
    }

    /**
     * Returns content as input stream. Be sure to close this stream properly!
     * 
     * @return input stream to read content from
     * @throws IOException 
     */
    public InputStream getInputStream() throws IOException {
        switch (getFormat()) {
        case JDOM: {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            XMLOutputter xout = new XMLOutputter(xmlFormat);
            xout.output(jdom, out);
            out.close();
            return new ByteArrayInputStream(out.toByteArray());
        }
        case DOM: {
            Document doc = new org.jdom.input.DOMBuilder().build(dom);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            XMLOutputter xout = new XMLOutputter(xmlFormat);
            xout.output(doc, out);
            out.close();
            return new ByteArrayInputStream(out.toByteArray());
        }
        default: {
            return in;
        }

        }
    }

    /**
     * Returns content as SAX input source.
     * 
     * @return input source to read content from
     * @throws IOException 
     */
    public InputSource getInputSource() throws IOException {
        InputSource source = new InputSource(getInputStream());
        source.setSystemId(systemId);
        return source;
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
     * Sends content to the given OutputStream
     * 
     * @param out
     *            the OutputStream to write the content to
     */
    public void sendTo(OutputStream out) throws IOException {
        switch (getFormat()) {
        case JDOM:
            XMLOutputter xout = new XMLOutputter(xmlFormat);
            xout.output(this.jdom, out);
            break;
        case DOM:
            try {
                Transformer xformer = TransformerFactory.newInstance().newTransformer();
                Result result = new StreamResult(out);
                xformer.transform(getSource(), result);
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw (IOException) e;
                }
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                new IOException(e);
            }
            break;
        default:
            InputStream input = getInputStream();
            MCRUtils.copyStream(input, out);
            input.close();
            break;
        }
    }

    /**
     * Sends content to the given local file
     * 
     * @param target
     *            the file to write the content to
     */
    public void sendTo(File target) throws IOException {
        OutputStream out = new FileOutputStream(target);
        sendTo(out);
        out.close();
    }

    /**
     * Sends the content to the given file object
     * 
     * @param target
     *            the file to write the content to
     */
    public void sendTo(FileObject target) throws IOException {
        OutputStream out = target.getContent().getOutputStream();
        sendTo(out);
        out.close();
    }

    /**
     * Parses content, assuming it is XML, and returns the parsed document.
     * 
     * @return the XML document parsed from content
     * @throws SAXParseException 
     * @throws MCRException 
     */
    public Document asXML() throws JDOMException, IOException, SAXParseException {
        switch (getFormat()) {
        case INPUT_STREAM: {
            return MCRXMLParserFactory.getNonValidatingParser().parseXML(this);
        }
        case DOM: {
            return new org.jdom.input.DOMBuilder().build(dom);
        }
        default: {
            return (Document) jdom.clone();
        }
        }
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
     * Returns the content as String, assuming the provided encoding
     * 
     * @param encoding
     *            the encoding to use to build the characters
     * @return content as String
     */
    public String asString(String encoding) throws IOException, UnsupportedEncodingException {
        return new String(asByteArray(), encoding);
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
     * Ensures that this content is not already consumed, because it can only be
     * used once.
     */
    protected void checkConsumed() {
        if (consumed) {
            throw new MCRUsageException("MCRContent is already consumed, can only be used once");
        } else {
            consumed = true;
        }
    }

    /**
     * Makes copies of the content, consuming this content
     * 
     * @param numCopies
     *            the number of copies to make
     * @return copies of the content
     */
    public MCRContent[] makeCopies(int numCopies) throws IOException {
        MCRContent[] copies = new MCRContent[numCopies];
        switch (getFormat()) {
        case JDOM: {
            for (int i = 0; i < numCopies; i++) {
                copies[i] = MCRContent.readFrom((Document) jdom.clone());
            }
            return copies;
        }
        case DOM: {
            for (int i = 0; i < numCopies; i++) {
                copies[i] = MCRContent.readFrom((org.w3c.dom.Document) dom.cloneNode(true));
            }
            return copies;
        }
        default: {
            byte[] bytes = asByteArray();
            for (int i = 0; i < numCopies; i++) {
                copies[i] = MCRContent.readFrom(bytes, systemId);
            }
            return copies;
        }
        }
    }

    public String getSystemId() {
        return systemId;
    }

    /**
     * Return the document type of the content
     * @return document type as String
     */
    public String getDocType() {
        switch (getFormat()) {
        case JDOM: {
            return jdom.getDocType() == null ? jdom.getRootElement().getName() : jdom.getDocType().getElementName();
        }
        case DOM: {
            return dom.getDoctype() == null ? dom.getDocumentElement().getLocalName() : dom.getDoctype().getName();
        }
        default: {
            String docType = MCRUtils.parseDocumentType(in);
            int pos = docType.indexOf(':') + 1;
            if (pos > 0) {
                //filter namespace prefix
                docType = docType.substring(pos);
            }
            return docType;
        }
        }	
    }

    /**
     * Return the content as Source
     * @return content as Source
     */
    public Source getSource() {
        switch (getFormat()) {
        case JDOM: {
            return new JDOMSource(jdom);
        }
        case DOM: {
            return new DOMSource(dom);
        }
        default: {
            return new StreamSource(in);
        }
        }
    }

    public MCRContentFormat getFormat() {
        return format;
    }
}
