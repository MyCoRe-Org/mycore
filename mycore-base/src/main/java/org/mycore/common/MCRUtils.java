/*
 * 
 * $Revision$ $Date$
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.common;

import static org.mycore.common.MCRConstants.DATE_FORMAT;
import static org.mycore.common.MCRConstants.DEFAULT_ENCODING;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs2.MCRMetadataStore;
import org.mycore.datamodel.ifs2.MCRMetadataVersion;
import org.mycore.datamodel.ifs2.MCRVersionedMetadata;
import org.mycore.datamodel.ifs2.MCRVersioningMetadataStore;
import org.mycore.datamodel.language.MCRLanguageFactory;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class represent a general set of external methods to support the
 * programming API.
 * 
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date: 2010-09-07 10:54:00 +0200 (Tue, 07 Sep
 *          2010) $
 */
public class MCRUtils {
    // The file slash
    private static String SLASH = System.getProperty("file.separator");;

    public final static char COMMAND_OR = 'O';

    public final static char COMMAND_AND = 'A';

    public final static char COMMAND_XOR = 'X';

    private static final int REV_LATEST = -1;

    // public constant data
    private static final Logger LOGGER = Logger.getLogger(MCRUtils.class);

    /**
     * This method check the language string base on RFC 1766 to the supported
     * languages in MyCoRe in a current application environment. Without appending 
     * this MCRLanguageFactory only ENGLISH and GERMAN are supported. 
     * 
     * @param code
     *            the language string in RFC 1766 syntax
     * @return true if the language code is supported. It return true too if the code starts
     *            with x- or i-, otherwise return false;
     * @deprecated use {@link MCRLanguageFactory.instance()#isSupportedLanguage(String)} instead
     */
    public static final boolean isSupportedLang(String code) {
        return MCRLanguageFactory.instance().isSupportedLanguage(code);
    }

    /**
     * The methode convert the input date string to the ISO output string. If
     * the input can't convert, the output is null.
     * 
     * @param indate
     *            the date input
     * @return the ISO output or null
     */
    public static final String convertDateToISO(String indate) {
        if (indate == null || (indate = indate.trim()).length() == 0) {
            return null;
        }

        GregorianCalendar calendar = new GregorianCalendar();
        boolean test = false;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        formatter.setLenient(false);

        try {
            calendar.setTime(formatter.parse(indate));
            test = true;
        } catch (ParseException e) {
        }

        if (!test) {
            for (DateFormat df : DATE_FORMAT) {
                df.setLenient(false);

                try {
                    calendar.setTime(df.parse(indate));
                    test = true;
                } catch (ParseException e) {
                }

                if (test) {
                    break;
                }
            }
        }

        if (!test) {
            return null;
        }

        formatter.setCalendar(calendar);

        return formatter.format(calendar.getTime());
    }

    /**
     * The methode convert the input date string to the GregorianCalendar. If
     * the input can't convert, the output is null.
     * 
     * @param indate
     *            the date input
     * @return the GregorianCalendar or null
     */
    public static final GregorianCalendar convertDateToGregorianCalendar(String indate) {
        if (indate == null || (indate = indate.trim()).length() == 0) {
            return null;
        }

        boolean era = true;
        int start = 0;

        if (indate.substring(0, 2).equals("AD")) {
            era = true;
            start = 2;
        }

        if (indate.substring(0, 2).equals("BC")) {
            era = false;
            start = 2;
        }

        GregorianCalendar calendar = new GregorianCalendar();
        boolean test = false;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        try {
            calendar.setTime(formatter.parse(indate.substring(start, indate.length())));

            if (!era) {
                calendar.set(Calendar.ERA, GregorianCalendar.BC);
            }

            test = true;
        } catch (ParseException e) {
        }

        if (!test) {
            for (DateFormat df : DATE_FORMAT) {
                try {
                    calendar.setTime(df.parse(indate.substring(start, indate.length())));

                    if (!era) {
                        calendar.set(Calendar.ERA, GregorianCalendar.BC);
                    }

                    test = true;
                } catch (ParseException e) {
                }

                if (test) {
                    break;
                }
            }
        }

        if (!test) {
            return null;
        }

        return calendar;
    }

    /**
     * This methode replace any characters to XML entity references.
     * <p>
     * <ul>
     * <li>&lt; to &amp;lt;
     * <li>&gt; to &amp;gt;
     * <li>&amp; to &amp;amp;
     * <li>&quot; to &amp;quot;
     * <li>&apos; to &amp;apos;
     * </ul>
     * 
     * @param in
     *            a string
     * @return the converted string.
     */
    public static final String stringToXML(String in) {
        if (in == null) {
            return "";
        }

        StringBuffer sb = new StringBuffer(2048);

        for (int i = 0; i < in.length(); i++) {
            if (in.charAt(i) == '<') {
                sb.append("&lt;");

                continue;
            }

            if (in.charAt(i) == '>') {
                sb.append("&gt;");

                continue;
            }

            if (in.charAt(i) == '&') {
                sb.append("&amp;");

                continue;
            }

            if (in.charAt(i) == '\"') {
                sb.append("&quot;");

                continue;
            }

            if (in.charAt(i) == '\'') {
                sb.append("&apos;");

                continue;
            }

            sb.append(in.charAt(i));
        }

        return sb.toString();
    }

    /**
     * This method convert a JDOM tree to a byte array.
     * 
     * @param jdom
     *            the JDOM tree
     *        format
     *            the JDOM output format    
     * @return a byte array of the JDOM tree
     */
    public static final byte[] getByteArray(org.jdom.Document jdom, Format format) throws MCRPersistenceException {
        MCRConfiguration conf = MCRConfiguration.instance();
        String mcr_encoding = conf.getString("MCR.Metadata.DefaultEncoding", DEFAULT_ENCODING);
        ByteArrayOutputStream outb = new ByteArrayOutputStream();

        try {
            XMLOutputter outp = new XMLOutputter(format.setEncoding(mcr_encoding));
            outp.output(jdom, outb);
        } catch (Exception e) {
            throw new MCRPersistenceException("Can't produce byte array.");
        }

        return outb.toByteArray();
    }

    /**
     * This method convert a JDOM tree to a byte array.
     * 
     * @param jdom
     *            the JDOM tree
     * @return a byte array of the JDOM tree
     */
    public static final byte[] getByteArray(org.jdom.Document jdom) throws MCRPersistenceException {
        return getByteArray(jdom, Format.getRawFormat());
    }

    /**
     * Converts an Array of Objects to an Array of Strings using the toString()
     * method.
     * 
     * @param objects
     *            Array of Objects to be converted
     * @return Array of Strings representing Objects
     */
    public static final String[] getStringArray(Object[] objects) {
        String[] returns = new String[objects.length];

        for (int i = 0; i < objects.length; i++) {
            returns[i] = objects[i].toString();
        }

        return returns;
    }

    /**
     * Converts an Array of Objects to an Array of Strings using the toString()
     * method.
     * 
     * @param objects
     *            Array of Objects to be converted
     * @param maxitems
     *            The maximum of items to convert
     * @return Array of Strings representing Objects
     */
    public static final String[] getStringArray(Object[] objects, int maxitems) {
        String[] returns = new String[maxitems];

        for (int i = 0; i < maxitems; i++) {
            returns[i] = objects[i].toString();
        }

        return returns;
    }

    /**
     * Copies all content read from the given input stream to the given output
     * stream. Note that this method will NOT close the streams when finished
     * copying.
     * 
     * @param source
     *            the InputStream to read the bytes from
     * @param target
     *            out the OutputStream to write the bytes to, may be null
     * @return true if Inputstream copied successfully to OutputStream
     */
    public static boolean copyStream(InputStream source, OutputStream target) {
        if (source == null) {
            throw new MCRException("InputStream source is null.");
        }

        try {
            // R E A D / W R I T E by chunks
            int chunkSize = 63 * 1024;

            // code will work even when chunkSize = 0 or chunks = 0;
            // Even for small files, we allocate a big buffer, since we
            // don't know the size ahead of time.
            byte[] ba = new byte[chunkSize];

            // keep reading till hit eof
            while (true) {
                int bytesRead = readBlocking(source, ba, 0, chunkSize);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(MCRUtils.class.getName() + ".copyStream(): " + bytesRead + " bytes read");
                }

                if (bytesRead > 0) {
                    if (target != null) {
                        target.write(ba, 0 /* offset in ba */, bytesRead /*
                                                                                                                     * bytes
                                                                                                                     * to
                                                                                                                     * write
                                                                                                                     */);
                    }
                } else {
                    break; // hit eof
                }
            } // end while

            // C L O S E, done by caller if wanted.
        } catch (IOException e) {
            LOGGER.debug("IOException caught while copying streams:");
            LOGGER.debug(e.getClass().getName() + ": " + e.getMessage());
            LOGGER.debug(e);
            return false;
        }

        // all was ok
        return true;
    } // end copy

    /**
     * Copies all content read from the given input stream to the given output
     * stream. Note that this method will NOT close the streams when finished
     * copying.
     * 
     * @param source
     *            the InputStream to read the bytes from
     * @param target
     *            out the OutputStream to write the bytes to, may be null
     * @return true if Inputstream copied successfully to OutputStream
     */
    public static boolean copyReader(Reader source, Writer target) {
        if (source == null) {
            throw new MCRException("Reader source is null.");
        }

        try {
            // R E A D / W R I T E by chunks
            int chunkSize = 63 * 1024;

            // code will work even when chunkSize = 0 or chunks = 0;
            // Even for small files, we allocate a big buffer, since we
            // don't know the size ahead of time.
            char[] ca = new char[chunkSize];

            // keep reading till hit eof
            while (true) {
                int charsRead = readBlocking(source, ca, 0, chunkSize);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(MCRUtils.class.getName() + ".copyReader(): " + charsRead + " characters read");
                }

                if (charsRead > 0) {
                    if (target != null) {
                        target.write(ca, 0 /* offset in ba */, charsRead /*
                                                                                                                     * bytes
                                                                                                                     * to
                                                                                                                     * write
                                                                                                                     */);
                    }
                } else {
                    break; // hit eof
                }
            } // end while

            // C L O S E, done by caller if wanted.
        } catch (IOException e) {
            return false;
        }

        // all was ok
        return true;
    } // end copy

    /**
     * merges to HashSets of MyCoreIDs after specific rules
     * 
     * @see #COMMAND_OR
     * @see #COMMAND_AND
     * @see #COMMAND_XOR
     * @param set1
     *            1st HashSet to be merged
     * @param set2
     *            2nd HashSet to be merged
     * @param operation
     *            available COMMAND_XYZ
     * @return merged HashSet
     */
    public static final <T> HashSet<T> mergeHashSets(HashSet<? extends T> set1, HashSet<? extends T> set2, char operation) {
        HashSet<T> merged = new HashSet<T>();
        T id;

        switch (operation) {
        case COMMAND_OR:
            merged.addAll(set1);
            merged.addAll(set2);

            break;

        case COMMAND_AND:

            for (Iterator<? extends T> it = set1.iterator(); it.hasNext();) {
                id = it.next();

                if (set2.contains(id)) {
                    merged.add(id);
                }
            }

            break;

        case COMMAND_XOR:

            for (Iterator<? extends T> it = set1.iterator(); it.hasNext();) {
                id = it.next();

                if (!set2.contains(id)) {
                    merged.add(id);
                }
            }

            for (Iterator<? extends T> it = set2.iterator(); it.hasNext();) {
                id = it.next();

                if (!set1.contains(id) && !merged.contains(id)) {
                    merged.add(id);
                }
            }

            break;

        default:
            throw new IllegalArgumentException("operation not permited: " + operation);
        }

        return merged;
    }

    /**
     * The method cut an ArrayList for a maximum of items.
     * 
     * @param arrayin
     *            The incoming ArrayList
     * @param maxitems
     *            The maximum number of items
     * @return the cutted ArrayList
     */
    public static final <T> ArrayList<T> cutArrayList(ArrayList<? extends T> arrayin, int maxitems) {
        if (arrayin == null) {
            throw new MCRException("Input ArrayList is null.");
        }

        if (maxitems < 1) {
            LOGGER.warn("The maximum items are lower then 1.");
        }

        ArrayList<T> arrayout = new ArrayList<T>();
        int i = 0;

        for (Iterator<? extends T> it = arrayin.iterator(); it.hasNext() && i < maxitems; i++) {
            arrayout.add(it.next());
        }
        return arrayout;
    }

    /**
     * Reads exactly <code>len</code> bytes from the input stream into the byte
     * array. This method reads repeatedly from the underlying stream until all
     * the bytes are read. InputStream.read is often documented to block like
     * this, but in actuality it does not always do so, and returns early with
     * just a few bytes. readBlockiyng blocks until all the bytes are read, the
     * end of the stream is detected, or an exception is thrown. You will always
     * get as many bytes as you asked for unless you get an eof or other
     * exception. Unlike readFully, you find out how many bytes you did get.
     * 
     * @param b
     *            the buffer into which the data is read.
     * @param off
     *            the start offset of the data.
     * @param len
     *            the number of bytes to read.
     * @return number of bytes actually read.
     * @exception IOException
     *                if an I/O error occurs.
     */
    public static final int readBlocking(InputStream in, byte[] b, int off, int len) throws IOException {
        int totalBytesRead = 0;

        while (totalBytesRead < len) {
            int bytesRead = in.read(b, off + totalBytesRead, len - totalBytesRead);

            if (bytesRead < 0) {
                break;
            }

            totalBytesRead += bytesRead;
        }

        return totalBytesRead;
    } // end readBlocking

    /**
     * Reads exactly <code>len</code> bytes from the input stream into the byte
     * array. This method reads repeatedly from the underlying stream until all
     * the bytes are read. Reader.read is often documented to block like this,
     * but in actuality it does not always do so, and returns early with just a
     * few bytes. readBlockiyng blocks until all the bytes are read, the end of
     * the stream is detected, or an exception is thrown. You will always get as
     * many bytes as you asked for unless you get an eof or other exception.
     * Unlike readFully, you find out how many bytes you did get.
     * 
     * @param c
     *            the buffer into which the data is read.
     * @param off
     *            the start offset of the data.
     * @param len
     *            the number of bytes to read.
     * @return number of bytes actually read.
     * @exception IOException
     *                if an I/O error occurs.
     */
    public static final int readBlocking(Reader in, char[] c, int off, int len) throws IOException {
        int totalCharsRead = 0;

        while (totalCharsRead < len) {
            int charsRead = in.read(c, off + totalCharsRead, len - totalCharsRead);

            if (charsRead < 0) {
                break;
            }

            totalCharsRead += charsRead;
        }

        return totalCharsRead;
    } // end readBlocking

    /**
     * <p>
     * Returns String in with newStr substituted for find String.
     * 
     * @param in
     *            String to edit
     * @param find
     *            string to match
     * @param newStr
     *            string to substitude for find
     */
    public static String replaceString(String in, String find, String newStr) {
        return in.replace(find, newStr);
    }

    /**
     * The method wrap the org.jdom.Element in a org.jdom.Document and write it
     * to a file.
     * 
     * @param elm
     *            the JDOM Document
     * @param xml
     *            the File instance
     */
    public static final void writeElementToFile(Element elm, File xml) {
        writeJDOMToFile(new Document().addContent(elm), xml);
    }

    /**
     * The method write a given JDOM Document to a file.
     * 
     * @param jdom
     *            the JDOM Document
     * @param xml
     *            the File instance
     */
    public static final void writeJDOMToFile(Document jdom, File xml) {
        try {
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(xml));
            xout.output(jdom, out);
            out.close();
        } catch (IOException ioe) {
            if (LOGGER.isDebugEnabled()) {
                ioe.printStackTrace();
            } else {
                LOGGER.error("Can't write org.jdom.Document to file " + xml.getName() + ".");
            }
        }
    }

    /**
     * The method wrap the org.jdom.Element in a org.jdom.Document and write it
     * to Sysout.
     * 
     * @param elm
     *            the JDOM Document
     */
    public static final void writeElementToSysout(Element elm) {
        writeJDOMToSysout(new Document().addContent(elm));
    }

    /**
     * The method write a given JDOM Document to the system output.
     * 
     * @param jdom
     *            the JDOM Document
     */
    public static final void writeJDOMToSysout(Document jdom) {
        try {
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            BufferedOutputStream out = new BufferedOutputStream(System.out);
            xout.output(jdom, out);
            out.flush();
        } catch (IOException ioe) {
            if (LOGGER.isDebugEnabled()) {
                ioe.printStackTrace();
            } else {
                LOGGER.error("Can't write org.jdom.Document to Sysout.");
            }
        }
    }

    /**
     * The method return a list of all file names under the given directory and
     * subdirectories of itself.
     * 
     * @param basedir
     *            the File instance of the basic directory
     * @return an ArrayList with file names as pathes
     */
    public static ArrayList<String> getAllFileNames(File basedir) {
        ArrayList<String> out = new ArrayList<String>();
        File[] stage = basedir.listFiles();

        for (File element : stage) {
            if (element.isFile()) {
                out.add(element.getName());
            }

            if (element.isDirectory()) {
                out.addAll(getAllFileNames(element, element.getName() + SLASH));
            }
        }

        return out;
    }

    /**
     * The method return a list of all file names under the given directory and
     * subdirectories of itself.
     * 
     * @param basedir
     *            the File instance of the basic directory
     * @param path
     *            the part of directory path
     * @return an ArrayList with file names as pathes
     */
    public static ArrayList<String> getAllFileNames(File basedir, String path) {
        ArrayList<String> out = new ArrayList<String>();
        File[] stage = basedir.listFiles();

        for (File element : stage) {
            if (element.isFile()) {
                out.add(path + element.getName());
            }

            if (element.isDirectory()) {
                out.addAll(getAllFileNames(element, path + element.getName() + SLASH));
            }
        }

        return out;
    }

    /**
     * The method return a list of all directory names under the given directory
     * and subdirectories of itself.
     * 
     * @param basedir
     *            the File instance of the basic directory
     * @return an ArrayList with directory names as pathes
     */
    public static ArrayList<String> getAllDirectoryNames(File basedir) {
        ArrayList<String> out = new ArrayList<String>();
        File[] stage = basedir.listFiles();

        for (File element : stage) {
            if (element.isDirectory()) {
                out.add(element.getName());
                out.addAll(getAllDirectoryNames(element, element.getName() + SLASH));
            }
        }

        return out;
    }

    /**
     * The method return a list of all directory names under the given directory
     * and subdirectories of itself.
     * 
     * @param basedir
     *            the File instance of the basic directory
     * @param path
     *            the part of directory path
     * @return an ArrayList with directory names as pathes
     */
    public static ArrayList<String> getAllDirectoryNames(File basedir, String path) {
        ArrayList<String> out = new ArrayList<String>();
        File[] stage = basedir.listFiles();

        for (File element : stage) {
            if (element.isDirectory()) {
                out.add(path + element.getName());
                out.addAll(getAllDirectoryNames(element, path + element.getName() + SLASH));
            }
        }

        return out;
    }

    /**
     * This method deletes a directory and all its content.
     * 
     * @param dir
     *            the File instance of the directory to delete
     * @return true if the directory was deleted successfully, otherwise false
     */
    public static boolean deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String element : children) {
                boolean success = deleteDirectory(new File(dir, element));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    public static String arrayToString(Object[] objArray, String seperator) {
        StringBuffer buf = new StringBuffer();

        for (Object element : objArray) {
            buf.append(element).append(seperator);
        }

        if (objArray.length > 0) {
            buf.setLength(buf.length() - seperator.length());
        }

        return buf.toString();
    }

    public static String parseDocumentType(InputStream in) {
        SAXParser parser = null;

        try {
            parser = SAXParserFactory.newInstance().newSAXParser();
        } catch (Exception ex) {
            String msg = "Could not build a SAX Parser for processing XML input";
            throw new MCRConfigurationException(msg, ex);
        }

        final Properties detected = new Properties();
        final String forcedInterrupt = "mcr.forced.interrupt";

        DefaultHandler handler = new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                LOGGER.debug("MCRLayoutService detected root element = " + qName);
                detected.setProperty("docType", qName);
                throw new MCRException(forcedInterrupt);
            }
        };

        try {
            parser.parse(new InputSource(in), handler);
        } catch (Exception ex) {
            if (!forcedInterrupt.equals(ex.getMessage())) {
                String msg = "Error while detecting XML document type from input source";
                throw new MCRException(msg, ex);
            }
        }

        String docType = detected.getProperty("docType");
        int pos = docType.indexOf(':') + 1;
        if (pos > 0) {
            //filter namespace prefix
            docType = docType.substring(pos);
        }
        return docType;

    }

    /**
     * Transforms the given Document into a String
     * 
     * @return the xml document as {@link String} or null if an
     *         {@link Exception} occurs
     */
    public static String documentAsString(Document doc) {
        String value = null;
        try {
            XMLOutputter op = new XMLOutputter(Format.getPrettyFormat());
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            op.output(doc, os);
            os.flush();
            value = new String(os.toByteArray());
            os.close();
        } catch (Exception e) {
            return null;
        }
        return value;
    }

    /**
     * Transforms the given Element into a String
     * 
     * @return the xml element as {@link String}
     * @throws IOException 
     */
    public static String asString(Element elm) throws IOException {
        XMLOutputter op = new XMLOutputter(Format.getPrettyFormat());
        StringWriter sw = new StringWriter();
        op.output(elm, sw);
        return sw.toString();
    }

    /**
     * @param objId
     *            the id of the object to be retrieved
     * @param revision
     *            the revision to be returned, specify -1 if you want to
     *            retrieve the latest revision (includes deleted objects also)
     * @return a {@link Document} representing the {@link MCRObject} of the
     *         given revision or <code>null</code> if there is no such object
     *         with the given revision
     * @throws IOException 
     * @throws JDOMException 
     * @throws SAXParseException 
     */
    public static Document requestVersionedObject(MCRObjectID objId, long revision) throws IOException, JDOMException, SAXParseException {
        MCRContent content = requestVersionedObjectAsContent(objId, revision);
        if (content == null) {
            return null;
        }
        return content.asXML();
    }

    /**
     * @param objId
     *            the id of the object to be retrieved
     * @param revision
     *            the revision to be returned, specify -1 if you want to
     *            retrieve the latest revision (includes deleted objects also)
     * @return a {@link MCRContent} representing the {@link MCRObject} of the
     *         given revision or <code>null</code> if there is no such object
     *         with the given revision
     * @throws IOException 
     */
    public static MCRContent requestVersionedObjectAsContent(MCRObjectID objId, long revision) throws IOException {
        LOGGER.info("Getting object " + objId + " in revision " + revision);
        MCRMetadataVersion version = getMetadataVersion(objId, revision);
        if (version != null) {
            return version.retrieve();
        }
        return null;
    }

    /**
     * Lists all versions of this metadata object available in the
     * subversion repository.
     * 
     * @param id
     *            the id of the object to be retrieved
     * @return {@link List} with all {@link MCRMetadataVersion} of
     *         the given object or null if the id is null or the metadata
     *         store doesn't support versioning
     * @throws IOException
     */
    public static List<MCRMetadataVersion> listRevisions(MCRObjectID id) throws IOException {
        if (id == null) {
            return null;
        }
        MCRMetadataStore metadataStore = MCRXMLMetadataManager.instance().getStore(id);
        if (!(metadataStore instanceof MCRVersioningMetadataStore)) {
            return null;
        }
        MCRVersioningMetadataStore verStore = (MCRVersioningMetadataStore) metadataStore;
        MCRVersionedMetadata vm = verStore.retrieve(id.getNumberAsInteger());
        return vm.listVersions();
    }

    /**
     * Returns the {@link MCRMetadataVersion} of the given id and revision.
     * 
     * @param mcrId
     *            the id of the object to be retrieved
     * @param rev
     *            the revision to be returned, specify -1 if you want to
     *            retrieve the latest revision (includes deleted objects also)
     * @return a {@link MCRMetadataVersion} representing the {@link MCRObject} of the
     *         given revision or <code>null</code> if there is no such object
     *         with the given revision
     * @throws IOException
     */
    public static MCRMetadataVersion getMetadataVersion(MCRObjectID mcrId, long rev) throws IOException {
        List<MCRMetadataVersion> versions = listRevisions(mcrId);
        if (versions == null) {
            return null;
        }
        if (rev == REV_LATEST && versions.size() > 0) {
            //request latest available revision
            MCRMetadataVersion lastVersion = versions.get(versions.size() - 1);
            if (lastVersion.getType() == MCRMetadataVersion.DELETED) {
                lastVersion = versions.get(versions.size() - 2);
            }
            return lastVersion;
        }
        for (MCRMetadataVersion version : versions) {
            //request specific revision
            if (version.getRevision() == rev) {
                return version;
            }
        }
        return null;
    }

    public static String asSHA1String(int iterations, byte[] salt, String text) throws NoSuchAlgorithmException {
        return getHash(iterations, salt, text, "SHA-1");
    }

    public static String asMD5String(int iterations, byte[] salt, String text) throws NoSuchAlgorithmException {
        return getHash(iterations, salt, text, "MD5");
    }

    public static String asCryptString(String salt, String text) {
        return MCRCrypt.crypt(salt, text);
    }

    private static String getHash(int iterations, byte[] salt, String text, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest digest;
        if (--iterations < 0) {
            iterations = 0;
        }
        byte[] data;
        try {
            digest = MessageDigest.getInstance(algorithm);
            text = Normalizer.normalize(text, Form.NFC);
            if (salt != null) {
                digest.update(salt);
            }
            data = digest.digest(text.getBytes("UTF-8"));
            for (int i = 0; i < iterations; i++) {
                data = digest.digest(data);
            }
        } catch (UnsupportedEncodingException e) {
            throw new MCRException("Could not get " + algorithm + " checksum", e);
        }
        return toHexString(data);
    }

    public static String toHexString(byte[] data) {
        return Hex.encodeHexString(data);
    }

    public static String toBase64String(byte[] data) {
        return Base64.encodeBase64String(data);
    }

    public static byte[] fromBase64String(String base64) {
        return Base64.decodeBase64(base64);
    }

    /**
     * @param fileInputStream
     * @return
     * @throws IOException 
     * @throws NoSuchAlgorithmException 
     */
    public static String getMD5Sum(InputStream fileInputStream) throws IOException, NoSuchAlgorithmException {
        byte[] digest;
        try {
            byte[] buffer = new byte[4096];
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            int numRead;
            do {
                numRead = fileInputStream.read(buffer);
                if (numRead > 0) {
                    md5Digest.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            digest = md5Digest.digest();
        } finally {
            try {
                fileInputStream.close();
            } catch (IOException e) {
                LOGGER.warn("Could not close Inputstream: " + fileInputStream);
            }
        }
        StringBuilder md5SumBuilder = new StringBuilder();
        for (byte b : digest) {
            md5SumBuilder.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        String md5Sum = md5SumBuilder.toString();
        return md5Sum;
    }
}
