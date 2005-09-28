/*
 * $RCSfile$
 * $Revision$ $Date$
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

package org.mycore.services.plugins;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRFileContentType;
import org.mycore.datamodel.ifs.MCRFileContentTypeFactory;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * Need to insert some things here
 * 
 */
public class OoTextPlugin implements TextFilterPlugin {
    private static final int MAJOR = 0;

    private static final int MINOR = 4;

    private static final EntityResolver OooResolver = new ResolveOfficeDTD();

    private static final String SAXparser = "org.apache.xerces.parsers.SAXParser";

    private static HashSet contentTypes;

    private static String info = null;

    private static int DEF_BYTE_SZ = 1024 * 63;

    private ByteArrayInputStream bis;

    /**
     * 
     */
    public OoTextPlugin() {
        super();

        if (contentTypes == null) {
            contentTypes = new HashSet();

            if (MCRFileContentTypeFactory.isTypeAvailable("sxw")) {
                contentTypes.add(MCRFileContentTypeFactory.getType("sxw"));
            }
        }

        try {
            Class.forName(SAXparser);
        } catch (ClassNotFoundException e) {
            throw new FilterPluginInstantiationException(new StringBuffer("This Plugin is only tested with Xerces").append("(http://xml.apache.org/xerces2-j/index.html) and").append(" though requires it to be installed somewhere in").append(" CLASSPATH. Please ensure that a jar file ").append(" containing the class ").append(SAXparser).append(" is listed in a CLASSPATH before running your")
                    .append(" brandnew MyCoRe(tm)-Application.\n").append(" I as a developer of cause know that Xerces is").append(" bundled with every MyCoRe(tm) release and thus").append(" you will never read this message.\n").append(" But just in case, I thought it is a good idea to").append(" implement this message here.").toString());
        }

        if (info == null) {
            info = new StringBuffer("This filter extracts the text out of a OpenOffice.org Text Document").toString();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.plugins.TextFilterPlugin#getName()
     */
    public String getName() {
        return "Yagee's amazing OpenOffice.org Text Filter";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.plugins.TextFilterPlugin#getInfo()
     */
    public String getInfo() {
        return info;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.plugins.TextFilterPlugin#getSupportedContentTypes()
     */
    public HashSet getSupportedContentTypes() {
        return contentTypes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.plugins.TextFilterPlugin#transform(org.mycore.datamodel.ifs.MCRFileContentType,org.mycore.datamodel.ifs.MCRContentInputStream,
     *      java.io.OutputStream)
     */
    public Reader transform(MCRFileContentType ct, InputStream input) throws FilterPluginTransformException {
        if (getSupportedContentTypes().contains(ct)) {
            try {
                System.out.println("Reading Oo-Document");

                return getTextReader(getXMLStream(input));
            } catch (SAXException e) {
                throw new FilterPluginTransformException("Error while parsing OpenOffice document.", e);
            } catch (IOException e) {
                throw new FilterPluginTransformException("Error while parsing OpenOffice document.", e);
            }
        } else {
            throw new FilterPluginTransformException("ContentType " + ct + " is not supported by " + getName() + "!");
        }
    }

    /**
     * @see org.mycore.services.plugins.TextFilterPlugin#getMajorNumber()
     */
    public int getMajorNumber() {
        return MAJOR;
    }

    /**
     * @see org.mycore.services.plugins.TextFilterPlugin#getMinorNumber()
     */
    public int getMinorNumber() {
        return MINOR;
    }

    private InputStream getXMLStream(InputStream inp) throws IOException {
        ZipInputStream zip = new ZipInputStream(inp);
        ZipEntry ze;

        // search for "content.xml" in ZipStream
        while ((ze = zip.getNextEntry()) != null) {
            if (ze.getName().equals("content.xml")) {
                break;
            }
        }

        if ((ze == null) || !ze.getName().equals("content.xml")) {
            throw new FilterPluginTransformException("No content.xml was found in OpenOffice.org document!");
        }

        int chunkSize = (ze.getSize() < 0) ? DEF_BYTE_SZ : (int) ze.getSize();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(chunkSize);
        BufferedOutputStream out = new BufferedOutputStream(bos);
        byte[] ba = new byte[chunkSize];

        while (true) {
            int bytesRead = MCRUtils.readBlocking(zip, ba, 0, chunkSize);

            if (bytesRead > 0) {
                out.write(ba, 0 /* offset in ba */, bytesRead /*
                                                                 * bytes to
                                                                 * write
                                                                 */);
            } else {
                break; // hit eof
            }
        }

        out.close();

        return new ByteArrayInputStream(bos.toByteArray());
    }

    private Reader getTextReader(InputStream xml) throws IOException, SAXException {
        XMLReader reader = XMLReaderFactory.createXMLReader(SAXparser);
        StringBuffer buf = new StringBuffer();
        reader.setContentHandler(new TextHandler(buf));

        InputSource inp = new InputSource(xml);
        reader.setEntityResolver(OooResolver);
        reader.parse(inp);

        return new StringBufferReader(buf);
    }

    private static class StringBufferReader extends Reader {
        private final StringBuffer buf;

        private int pos;

        public StringBufferReader(StringBuffer buf) {
            this.buf = buf;
            pos = 0;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.io.Reader#close()
         */
        public void close() throws IOException {
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.io.Reader#read(char[], int, int)
         */
        public int read(char[] cbuf, int off, int len) throws IOException {
            if (pos == buf.length()) {
                return -1;
            } else {
                int start = pos + off;
                int charsRead = (buf.length() < (start + len)) ? (buf.length() - start) : len;
                int end = start + charsRead;
                buf.getChars(start, end, cbuf, 0);
                pos = end;

                return charsRead;
            }
        }
    }

    private static class TextHandler extends DefaultHandler {
        private static String textNS = "http://openoffice.org/2000/text";

        private final StringBuffer buf;

        private boolean textElement = false;

        private TextHandler(StringBuffer buf) {
            this.buf = buf;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.xml.sax.ContentHandler#characters(char[], int, int)
         */
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (textElement) {
                // write text to the stream
                buf.append(ch, start, length).append(' ');
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
         *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            // using internal optimized Strings of Xerces-J
            if (uri == textNS) {
                textElement = true;
            } else if (uri.equals(textNS)) {
                textElement = true;

                // therefor we might need to assign a given uri to textNS
                textNS = uri;
            } else {
                textElement = false;
            }
        }
    }

    public static class ResolveOfficeDTD implements EntityResolver {
        /**
         * returns an empty dtd to parse Ooo documents we don't need them, since
         * we validate them either
         */
        public InputSource resolveEntity(String publicId, String systemId) {
            return new InputSource(new StringReader(" "));
        }
    }
}
