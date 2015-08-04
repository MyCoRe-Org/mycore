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

import static org.mycore.common.MCRConstants.DEFAULT_ENCODING;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.streams.MCRDevNull;
import org.mycore.common.content.streams.MCRMD5InputStream;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
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
    private static String SLASH = System.getProperty("file.separator");

    public final static char COMMAND_OR = 'O';

    public final static char COMMAND_AND = 'A';

    public final static char COMMAND_XOR = 'X';

    // public constant data
    private static final Logger LOGGER = Logger.getLogger(MCRUtils.class);

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
    public static String stringToXML(String in) {
        if (in == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder(2048);

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
    public static byte[] getByteArray(org.jdom2.Document jdom, Format format) throws MCRPersistenceException {
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
    public static byte[] getByteArray(org.jdom2.Document jdom) throws MCRPersistenceException {
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
    public static String[] getStringArray(Object[] objects) {
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
    public static String[] getStringArray(Object[] objects, int maxitems) {
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
     * @deprecated Use {@link IOUtils#copy(InputStream, OutputStream)} instead
     */
    public static boolean copyStream(InputStream source, OutputStream target) {
        if (source == null) {
            throw new MCRException("InputStream source is null.");
        }
        try {
            int bytesRead = IOUtils.copy(source, target);
            LOGGER.debug("copyStream(): " + bytesRead + " bytes read");
            // C L O S E, done by caller if wanted.
        } catch (IOException e) {
            LOGGER.error("IOException caught while copying streams:", e);
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
     * @deprecated Use {@link IOUtils#copy(Reader, Writer)} instead
     */
    public static boolean copyReader(Reader source, Writer target) {
        if (source == null) {
            throw new MCRException("Reader source is null.");
        }

        try {
            int bytesRead = IOUtils.copy(source, target);
            LOGGER.debug("copyReader(): " + bytesRead + " bytes read");
        } catch (IOException e) {
            LOGGER.error("IOException while copy reader:", e);
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
    public static <T> HashSet<T> mergeHashSets(HashSet<? extends T> set1, HashSet<? extends T> set2, char operation) {
        HashSet<T> merged = new HashSet<T>();
        T id;

        switch (operation) {
        case COMMAND_OR:
            merged.addAll(set1);
            merged.addAll(set2);

            break;

        case COMMAND_AND:

            for (T aSet11 : set1) {
                id = aSet11;

                if (set2.contains(id)) {
                    merged.add(id);
                }
            }

            break;

        case COMMAND_XOR:

            for (T aSet1 : set1) {
                id = aSet1;

                if (!set2.contains(id)) {
                    merged.add(id);
                }
            }

            for (T aSet2 : set2) {
                id = aSet2;

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
    public static <T> ArrayList<T> cutArrayList(ArrayList<? extends T> arrayin, int maxitems) {
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
    public static int readBlocking(InputStream in, byte[] b, int off, int len) throws IOException {
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
    public static int readBlocking(Reader in, char[] c, int off, int len) throws IOException {
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
     * The method wrap the org.jdom2.Element in a org.jdom2.Document and write it
     * to a file.
     * 
     * @param elm
     *            the JDOM Document
     * @param xml
     *            the File instance
     */
    public static void writeElementToFile(Element elm, File xml) {
        writeJDOMToFile(new Document().addContent(elm), xml);
    }

    /**
     * Writes plain text to a file.
     * 
     * @param textToWrite
     *            the text to write into the file
     * @param fileName
     *            the name of the file to write to, given as absolute path
     * @return a handle to the written file
     * @throws IOException
     */
    public static Path writeTextToFile(String textToWrite, String fileName, Charset cs) throws IOException {
        Path file = Paths.get(fileName);
        Files.write(file, Arrays.asList(textToWrite), cs, StandardOpenOption.CREATE);
        return file;
    }

    /**
     * The method write a given JDOM Document to a file.
     * 
     * @param jdom
     *            the JDOM Document
     * @param xml
     *            the File instance
     */
    public static void writeJDOMToFile(Document jdom, File xml) {
        try {
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(xml));
            xout.output(jdom, out);
            out.close();
        } catch (IOException ioe) {
            if (LOGGER.isDebugEnabled()) {
                ioe.printStackTrace();
            } else {
                LOGGER.error("Can't write org.jdom2.Document to file " + xml.getName() + ".");
            }
        }
    }

    /**
     * The method wrap the org.jdom2.Element in a org.jdom2.Document and write it
     * to Sysout.
     * 
     * @param elm
     *            the JDOM Document
     */
    public static void writeElementToSysout(Element elm) {
        writeJDOMToSysout(new Document().addContent(elm));
    }

    /**
     * The method write a given JDOM Document to the system output.
     * 
     * @param jdom
     *            the JDOM Document
     */
    public static void writeJDOMToSysout(Document jdom) {
        try {
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            BufferedOutputStream out = new BufferedOutputStream(System.out);
            xout.output(jdom, out);
            out.flush();
        } catch (IOException ioe) {
            if (LOGGER.isDebugEnabled()) {
                ioe.printStackTrace();
            } else {
                LOGGER.error("Can't write org.jdom2.Document to Sysout.");
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
        StringBuilder buf = new StringBuilder();

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
     * Transforms the given {@link Document} into a String
     * 
     * @return the xml document as {@link String} or null if an
     *         {@link Exception} occurs
     *         
     * @deprecated please use {@link MCRUtils#asString(Document)}
     */
    @Deprecated
    public static String documentAsString(Document doc) {
        return asString(doc);
    }

    /**
     * Transforms the given {@link Document} into a String
     * 
     * @return the xml document as {@link String} or null if an
     *         {@link Exception} occurs
     */
    public static String asString(Document doc) {
        XMLOutputter op = new XMLOutputter(Format.getPrettyFormat());
        return op.outputString(doc);
    }

    /**
     * Transforms the given {@link Element} into a String
     * 
     * @return the element as {@link String}
     */
    @Deprecated
    public static String elementAsString(Element element) {
        return asString(element);
    }

    /**
     * Transforms the given Element into a String
     * 
     * @return the xml element as {@link String}
     * @throws IOException 
     */
    public static String asString(Element elm) {
        XMLOutputter op = new XMLOutputter(Format.getPrettyFormat());
        return op.outputString(elm);
    }

    public static String asSHA1String(int iterations, byte[] salt, String text) throws NoSuchAlgorithmException {
        return getHash(iterations, salt, text, "SHA-1");
    }

    public static String asSHA256String(int iterations, byte[] salt, String text) throws NoSuchAlgorithmException {
        return getHash(iterations, salt, text, "SHA-256");
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
        return DatatypeConverter.printHexBinary(data).toLowerCase(Locale.ROOT);
    }

    public static String toBase64String(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] fromBase64String(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Calculates md5 sum of InputStream.
     * 
     * InputStream is consumed after calling this method and automatically closed.
     * @param inputStream
     * @return
     * @throws IOException 
     * @throws NoSuchAlgorithmException 
     */
    public static String getMD5Sum(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        MCRMD5InputStream md5InputStream = null;
        try {
            md5InputStream = new MCRMD5InputStream(inputStream);
            IOUtils.copy(md5InputStream, new MCRDevNull());
            return md5InputStream.getMD5String();
        } finally {
            if (md5InputStream != null) {
                md5InputStream.close();
            }
        }
    }

    /**
     * Extracts files in a tar archive. Currently works only on uncompressed tar files.
     * 
     * @param source the uncompressed tar to extract
     * @param expandToDirectory the directory to extract the tar file to
     * 
     * @throws IOException if the source file does not exists
     * 
     * @author shermann
     */
    public static void untar(File source, File expandToDirectory) throws IOException {
        TarArchiveInputStream tarOutputStream = new TarArchiveInputStream(new FileInputStream(source));
        try {
            ArchiveEntry entry = null;
            byte[] buffer = new byte[10240];

            while ((entry = tarOutputStream.getNextTarEntry()) != null) {
                String fileName = entry.getName();
                File newFile = new File(expandToDirectory.getAbsolutePath() + File.separator + fileName);

                if (entry.isDirectory()) {
                    new File(newFile.getParent()).mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fileOutputStream = new FileOutputStream(newFile)) {
                        int length = -1;
                        while ((length = tarOutputStream.read(buffer)) > 0) {
                            fileOutputStream.write(buffer, 0, length);
                        }
                    }
                }
            }
        } finally {
            tarOutputStream.close();
        }
    }

    @SafeVarargs
    public static Exception unwrapExCeption(Exception e, Class<? extends Exception>... classes) {
        if (classes.length == 0) {
            return e;
        }
        Class<? extends Exception> mainExceptionClass = classes[0];
        Throwable check = e;
        for (Class<? extends Exception> instChk : classes) {
            if (instChk.isInstance(check)) {
                return (Exception) check;
            }
            check = check.getCause();
            if (check == null) {
                break;
            }
        }
        @SuppressWarnings("unchecked")
        Constructor<? extends Exception>[] constructors = (Constructor<? extends Exception>[]) mainExceptionClass.getConstructors();
        for (Constructor<? extends Exception> c : constructors) {
            Class<?>[] parameterTypes = c.getParameterTypes();
            try {
                if (parameterTypes.length == 0) {
                    Exception exception = c.newInstance((Object[]) null);
                    exception.initCause(e);
                    return exception;
                }
                if (parameterTypes.length == 1 && parameterTypes[0].isAssignableFrom(mainExceptionClass)) {
                    return c.newInstance(e);
                }
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                LOGGER.warn("Exception while initializing exception " + mainExceptionClass.getCanonicalName(), ex);
                return e;
            }
        }
        LOGGER.warn("Could not instanciate Exception " + mainExceptionClass.getCanonicalName());
        return e;
    }

    /**
     * Takes a file size in bytes and formats it as a string for output. For
     * values &lt; 5 KB the output format is for example "320 Byte". For values
     * &gt; 5 KB the output format is for example "6,8 KB". For values &gt; 1 MB
     * the output format is for example "3,45 MB".
     */
    public static String getSizeFormatted(long bytes) {
        String sizeUnit;
        String sizeText;
        double sizeValue;

        if (bytes >= 1024 * 1024) // >= 1 MB
        {
            sizeUnit = "MB";
            sizeValue = (double) Math.round(bytes / 10485.76) / 100;
        } else if (bytes >= 5 * 1024) // >= 5 KB
        {
            sizeUnit = "KB";
            sizeValue = (double) Math.round(bytes / 102.4) / 10;
        } else // < 5 KB
        {
            sizeUnit = "Byte";
            sizeValue = bytes;
        }

        sizeText = String.valueOf(sizeValue).replace('.', ',');

        if (sizeText.endsWith(",0")) {
            sizeText = sizeText.substring(0, sizeText.length() - 2);
        }

        return sizeText + " " + sizeUnit;
    }

}
