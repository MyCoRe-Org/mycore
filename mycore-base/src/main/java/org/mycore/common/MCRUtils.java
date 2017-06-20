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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.streams.MCRDevNull;
import org.mycore.common.content.streams.MCRMD5InputStream;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.niofs.MCRPathUtils;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class represent a general set of external methods to support the programming API.
 * 
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRUtils {
    // The file slash
    private static String SLASH = System.getProperty("file.separator");

    @Deprecated
    public final static char COMMAND_OR = 'O';

    @Deprecated
    public final static char COMMAND_AND = 'A';

    @Deprecated
    public final static char COMMAND_XOR = 'X';

    // public constant data
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * This methode replace any characters to XML entity references.
     * <p>
     * <ul>
     * <li>&lt; to &amp;lt;
     * <li>&gt; to &amp;gt;
     * <li>&amp; to &amp;amp;
     * <li>&quot; to &amp;quot;
     * <li>' to &amp;apos;
     * </ul>
     * 
     * @param in
     *            a string
     * @return the converted string.
     * @deprecated as it is not used anymore
     */
    @Deprecated
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
     *            the JDOM tree format the JDOM output format
     * @return a byte array of the JDOM tree
     * @deprecated use {@link MCRJDOMContent#asByteArray()}
     */
    @Deprecated
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
     * @deprecated uses {@link MCRJDOMContent#asByteArray()}
     */
    @Deprecated
    public static byte[] getByteArray(org.jdom2.Document jdom) throws MCRPersistenceException {
        return getByteArray(jdom, Format.getRawFormat());
    }

    /**
     * Converts an Array of Objects to an Array of Strings using the toString() method.
     * 
     * @param objects
     *            Array of Objects to be converted
     * @return Array of Strings representing Objects
     * @deprecated use {@link Stream}s to convert arrays
     */
    @Deprecated
    public static String[] getStringArray(Object[] objects) {
        String[] returns = new String[objects.length];

        for (int i = 0; i < objects.length; i++) {
            returns[i] = objects[i].toString();
        }

        return returns;
    }

    /**
     * Converts an Array of Objects to an Array of Strings using the toString() method.
     * 
     * @param objects
     *            Array of Objects to be converted
     * @param maxitems
     *            The maximum of items to convert
     * @return Array of Strings representing Objects
     * @deprecated use {@link Stream}s to convert arrays
     */
    public static String[] getStringArray(Object[] objects, int maxitems) {
        String[] returns = new String[maxitems];

        for (int i = 0; i < maxitems; i++) {
            returns[i] = objects[i].toString();
        }

        return returns;
    }

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
     * @deprecated use {@link Stream}s for this
     */
    @Deprecated
    public static <T> HashSet<T> mergeHashSets(HashSet<? extends T> set1, HashSet<? extends T> set2, char operation) {
        Predicate<T> inSet1 = set1::contains;
        Predicate<T> inSet2 = set2::contains;
        Predicate<T> op;

        switch (operation) {
            case COMMAND_OR:
                op = t -> true;//inSet1.or(inSet2);
                break;

            case COMMAND_AND:
                op = inSet1.and(inSet2);
                break;

            case COMMAND_XOR:
                op = inSet1.and(inSet2).negate();
                break;

            default:
                throw new IllegalArgumentException("operation not permited: " + operation);
        }

        return Stream
            .concat(set1.stream(), set2.stream())
            .filter(op)
            .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * The method cut an ArrayList for a maximum of items.
     * 
     * @param arrayin
     *            The incoming ArrayList
     * @param maxitems
     *            The maximum number of items
     * @return the cutted ArrayList
     * @deprecated use {@link List#subList(int, int)} instead
     */
    @Deprecated
    public static <T> ArrayList<T> cutArrayList(ArrayList<T> arrayin, int maxitems) {
        return new ArrayList<>(arrayin.subList(0, Math.max(0, maxitems)));
    }

    /**
     * Reads exactly <code>len</code> bytes from the input stream into the byte array. This method reads repeatedly from
     * the underlying stream until all the bytes are read. InputStream.read is often documented to block like this, but
     * in actuality it does not always do so, and returns early with just a few bytes. readBlockiyng blocks until all
     * the bytes are read, the end of the stream is detected, or an exception is thrown. You will always get as many
     * bytes as you asked for unless you get an eof or other exception. Unlike readFully, you find out how many bytes
     * you did get.
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
     * Reads exactly <code>len</code> bytes from the input stream into the byte array. This method reads repeatedly from
     * the underlying stream until all the bytes are read. Reader.read is often documented to block like this, but in
     * actuality it does not always do so, and returns early with just a few bytes. readBlockiyng blocks until all the
     * bytes are read, the end of the stream is detected, or an exception is thrown. You will always get as many bytes
     * as you asked for unless you get an eof or other exception. Unlike readFully, you find out how many bytes you did
     * get.
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
     * @deprecated use {@link MCRXMLFunctions#regexp(String, String, String)}
     */
    @Deprecated
    public static String replaceString(String in, String find, String newStr) {
        return in.replace(find, newStr);
    }

    /**
     * The method wrap the org.jdom2.Element in a org.jdom2.Document and write it to a file.
     * 
     * @param elm
     *            the JDOM Document
     * @param xml
     *            the File instance
     * @deprecated use {@link MCRJDOMContent#sendTo(Path, java.nio.file.CopyOption...)}
     */
    @Deprecated
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
     * @deprecated use {@link MCRJDOMContent#sendTo(Path, java.nio.file.CopyOption...)}
     */
    @Deprecated
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
     * The method wrap the org.jdom2.Element in a org.jdom2.Document and write it to Sysout.
     * 
     * @param elm
     *            the JDOM Document
     * @deprecated use {@link MCRJDOMContent#sendTo(java.io.OutputStream)}
     */
    @Deprecated
    public static void writeElementToSysout(Element elm) {
        writeJDOMToSysout(new Document().addContent(elm));
    }

    /**
     * The method write a given JDOM Document to the system output.
     * 
     * @param jdom
     *            the JDOM Document
     * @deprecated use {@link MCRJDOMContent#sendTo(java.io.OutputStream)}
     */
    @Deprecated
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
     * The method return a list of all file names under the given directory and subdirectories of itself.
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
     * The method return a list of all file names under the given directory and subdirectories of itself.
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
     * The method return a list of all directory names under the given directory and subdirectories of itself.
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
     * The method return a list of all directory names under the given directory and subdirectories of itself.
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
     * @deprecated use {@link MCRRecursiveDeleter} instead
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

    /**
     * @deprecated use {@link Stream#collect(java.util.stream.Collector)} instead
     */
    @Deprecated
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
     * @return the xml document as {@link String} or null if an {@link Exception} occurs
     * @deprecated use {@link XMLOutputter} directly
     */
    @Deprecated
    public static String asString(Document doc) {
        return new XMLOutputter(Format.getPrettyFormat()).outputString(doc);
    }

    /**
     * Transforms the given Element into a String
     * 
     * @return the xml element as {@link String}
     * @deprecated use {@link XMLOutputter} directly
     */
    @Deprecated
    public static String asString(Element elm) {
        return new XMLOutputter(Format.getPrettyFormat()).outputString(elm);
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

    private static String getHash(int iterations, byte[] salt, String text, String algorithm)
        throws NoSuchAlgorithmException {
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

    /**
     * @deprecated use {@link Base64} instead
     */
    @Deprecated
    public static String toBase64String(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * @deprecated use {@link Base64} instead
     */
    @Deprecated
    public static byte[] fromBase64String(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Calculates md5 sum of InputStream. InputStream is consumed after calling this method and automatically closed.
     */
    public static String getMD5Sum(InputStream inputStream) throws IOException {
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
     * @deprecated use {@link #untar(Path, Path)} instead
     */
    @Deprecated
    public static void untar(File source, File expandToDirectory) throws IOException {
        untar(source.toPath(), expandToDirectory.toPath());
    }

    /**
     * Extracts files in a tar archive. Currently works only on uncompressed tar files.
     * 
     * @param source
     *            the uncompressed tar to extract
     * @param expandToDirectory
     *            the directory to extract the tar file to
     * @throws IOException
     *             if the source file does not exists
     */
    public static void untar(Path source, Path expandToDirectory) throws IOException {
        try (TarArchiveInputStream tain = new TarArchiveInputStream(Files.newInputStream(source))) {
            TarArchiveEntry tarEntry;
            FileSystem targetFS = expandToDirectory.getFileSystem();
            HashMap<Path, FileTime> directoryTimes = new HashMap<>();
            while ((tarEntry = tain.getNextTarEntry()) != null) {
                Path target = MCRPathUtils.getPath(targetFS, tarEntry.getName());
                Path absoluteTarget = expandToDirectory.resolve(target).normalize().toAbsolutePath();
                if (tarEntry.isDirectory()) {
                    Files.createDirectories(expandToDirectory.resolve(absoluteTarget));
                    directoryTimes.put(absoluteTarget, FileTime.fromMillis(tarEntry.getLastModifiedDate().getTime()));
                } else {
                    if (Files.notExists(absoluteTarget.getParent())) {
                        Files.createDirectories(absoluteTarget.getParent());
                    }
                    Files.copy(tain, absoluteTarget, StandardCopyOption.REPLACE_EXISTING);
                    Files.setLastModifiedTime(absoluteTarget,
                        FileTime.fromMillis(tarEntry.getLastModifiedDate().getTime()));
                }
            }
            //restore directory dates
            Files.walkFileTree(expandToDirectory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Path absolutePath = dir.normalize().toAbsolutePath();
                    Files.setLastModifiedTime(absolutePath, directoryTimes.get(absolutePath));
                    return super.postVisitDirectory(dir, exc);
                }
            });
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
        Constructor<? extends Exception>[] constructors = (Constructor<? extends Exception>[]) mainExceptionClass
            .getConstructors();
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
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException ex) {
                LOGGER.warn("Exception while initializing exception " + mainExceptionClass.getCanonicalName(), ex);
                return e;
            }
        }
        LOGGER.warn("Could not instanciate Exception " + mainExceptionClass.getCanonicalName());
        return e;
    }

    /**
     * Takes a file size in bytes and formats it as a string for output. For values &lt; 5 KB the output format is for
     * example "320 Byte". For values &gt; 5 KB the output format is for example "6,8 KB". For values &gt; 1 MB the
     * output format is for example "3,45 MB".
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

    /**
     * Helps to implement {@link Comparable#compareTo(Object)}
     *
     * For every <code>part</code> a check is performed in the specified order.
     * The first check that does not return <code>0</code> the result is returned
     * by this method. So when this method returns <code>0</code> <code>first</code>
     * and <code>other</code> should be the same.
     *
     * @param first first Object that should be compared
     * @param other Object that first should be compared against, e.g. <code>first.compareTo(other)</code>
     * @param part different <code>compareTo()</code> steps
     * @param <T> object that wants to implement compareTo()
     * @return  a negative integer, zero, or a positive integer as this object
     *          is less than, equal to, or greater than the specified object.
     *
     * @throws NullPointerException if either <code>first</code> or <code>other</code> is null
     */
    @SafeVarargs
    public static <T> int compareParts(T first, T other, Function<T, Comparable>... part) {
        return Stream.of(part)
            .mapToInt(f -> f.apply(first).compareTo(f.apply(other)))
            .filter(i -> i != 0)
            .findFirst()
            .orElse(0);
    }
}
