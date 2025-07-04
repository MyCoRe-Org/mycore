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

package org.mycore.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.streams.MCRDevNull;
import org.mycore.common.content.streams.MCRDigestInputStream;
import org.mycore.common.digest.MCRDigest;
import org.mycore.common.digest.MCRMD5Digest;
import org.mycore.common.function.MCRThrowableTask;
import org.mycore.datamodel.niofs.MCRPathUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class represent a general set of external methods to support the programming API.
 *
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRUtils {
    // public constant data
    private static final Logger LOGGER = LogManager.getLogger();

    // The file slash
    private static final String SLASH = FileSystems.getDefault().getSeparator();

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
     * Writes plain text to a file.
     *
     * @param textToWrite the text to write into the file
     * @param fileName    the name of the file to write to, given as absolute path
     * @return a handle to the written file
     */
    public static Path writeTextToFile(String textToWrite, String fileName, Charset cs) throws IOException {
        Path file = Paths.get(fileName);
        Files.write(file, Collections.singletonList(textToWrite), cs, StandardOpenOption.CREATE);
        return file;
    }

    /**
     * The method return a list of all file names under the given directory and subdirectories of itself.
     *
     * @param basedir
     *            the File instance of the basic directory
     * @return an ArrayList with file names as pathes
     */
    public static List<String> getAllFileNames(File basedir) {
        List<String> out = new ArrayList<>();
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
     * @param basedir the File instance of the basic directory
     * @param path    the part of directory path
     * @return an ArrayList with file names as pathes
     */
    public static List<String> getAllFileNames(File basedir, String path) {
        List<String> out = new ArrayList<>();
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
    public static List<String> getAllDirectoryNames(File basedir) {
        List<String> out = new ArrayList<>();
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
    public static List<String> getAllDirectoryNames(File basedir, String path) {
        List<String> out = new ArrayList<>();
        File[] stage = basedir.listFiles();

        for (File element : stage) {
            if (element.isDirectory()) {
                out.add(path + element.getName());
                out.addAll(getAllDirectoryNames(element, path + element.getName() + SLASH));
            }
        }

        return out;
    }

    public static String parseDocumentType(InputStream in) {
        SAXParser parser;

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
                LOGGER.debug("MCRLayoutService detected root element = {}", qName);
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

    public static String asSHA1String(int iterations, byte[] salt, String text) throws NoSuchAlgorithmException {
        return getHash(iterations, salt, text, "SHA-1");
    }

    public static String asSHA256String(int iterations, byte[] salt, String text) throws NoSuchAlgorithmException {
        return getHash(iterations, salt, text, "SHA-256");
    }

    public static String asSHA512String(int iterations, byte[] salt, String text) throws NoSuchAlgorithmException {
        return getHash(iterations, salt, text, "SHA-512");
    }

    public static String asMD5String(int iterations, byte[] salt, String text) throws NoSuchAlgorithmException {
        return getHash(iterations, salt, text, "MD5");
    }

    public static String asCryptString(String salt, String text) {
        return MCRCrypt.crypt(salt, text);
    }

    /**
     * Hashes string specified by alogrithm.
     *
     * @param text input
     * @param algorithm hash algorithm
     * @param salt salt
     * @param iterations hash iterations, 1 is default
     * @return hashed string as hex string
     * @throws MCRException is hashing failed
     */
    public static String hashString(String text, String algorithm, byte[] salt, int iterations) {
        try {
            return getHash(iterations, salt, text, algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new MCRException("Unable to hash string", e);
        }
    }

    private static String getHash(int iterations, byte[] salt, String text, String algorithm)
        throws NoSuchAlgorithmException {
        MessageDigest digest;
        int currentIndex = iterations - 1;
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        byte[] data;
        digest = MessageDigest.getInstance(algorithm);
        String textNormalized = Normalizer.normalize(text, Form.NFC);
        if (salt != null) {
            digest.update(salt);
        }
        data = digest.digest(textNormalized.getBytes(StandardCharsets.UTF_8));

        for (int i = 0; i < currentIndex; i++) {
            data = digest.digest(data);
        }
        return toHexString(data);
    }

    public static String toHexString(byte[] data) {
        return HexFormat.of().formatHex(data);
    }

    /**
     * Builds a MessageDigest instance for checksum computation.
     *
     * @throws MCRConfigurationException if no java classes supports the given digestAlgorithm algorithm
     */
    public static MessageDigest buildMessageDigest(MCRDigest.Algorithm digestAlgorithm) {
        try {
            return MessageDigest.getInstance(digestAlgorithm.toUpperCase());
        } catch (NoSuchAlgorithmException exc) {
            throw new MCRConfigurationException("Could not find java classes that support " +
                digestAlgorithm.toUpperCase() + " checksum algorithm", exc);
        }
    }

    /**
     * Calculates digest sum of InputStream. InputStream is consumed after calling this method and automatically closed.
     */
    public static String getDigest(MCRDigest.Algorithm algorithm, InputStream inputStream) throws IOException {
        try (MCRDigestInputStream digestInputStream = new MCRDigestInputStream(inputStream, algorithm)) {
            digestInputStream.transferTo(new MCRDevNull());
            return digestInputStream.getDigestAsHexString();
        }
    }

    /**
     * Calculates md5 sum of InputStream. InputStream is consumed after calling this method and automatically closed.
     */
    public static String getMD5Sum(InputStream inputStream) throws IOException {
        return getDigest(MCRMD5Digest.ALGORITHM, inputStream);
    }

    /**
     * Extracts files in a tar archive. Currently, works only on uncompressed tar files.
     *
     * @param source
     *            the uncompressed tar to extract
     * @param expandToDirectory
     *            the directory to extract the tar file to
     * @throws IOException
     *             if the source file does not exist
     */
    public static void untar(Path source, Path expandToDirectory) throws IOException {
        try (TarArchiveInputStream tain = new TarArchiveInputStream(Files.newInputStream(source))) {
            FileSystem targetFS = expandToDirectory.getFileSystem();
            Map<Path, FileTime> directoryTimes = new HashMap<>();
            TarArchiveEntry tarEntry = tain.getNextEntry();
            while (tarEntry != null) {
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
                tarEntry = tain.getNextEntry();
            }
            //restore directory dates
            Files.walkFileTree(expandToDirectory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Path absolutePath = dir.normalize().toAbsolutePath();
                    FileTime lastModifiedTime = directoryTimes.get(absolutePath);
                    if (lastModifiedTime != null) {
                        Files.setLastModifiedTime(absolutePath, lastModifiedTime);
                    } else {
                        LOGGER.warn("Could not restore last modified time for {} from TAR file {}.", absolutePath,
                            source);
                    }
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
                LOGGER.warn(() -> "Exception while initializing exception " + mainExceptionClass.getCanonicalName(),
                    ex);
                return e;
            }
        }
        LOGGER.warn("Could not instantiate Exception {}", mainExceptionClass::getCanonicalName);
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

        if (bytes >= 1024 * 1024) {
            // >= 1 MB
            sizeUnit = "MB";
            sizeValue = (double) Math.round(bytes / 10_485.76) / 100;
        } else if (bytes >= 5 * 1024) {
            // >= 5 KB
            sizeUnit = "KB";
            sizeValue = (double) Math.round(bytes / 102.4) / 10;
        } else {
            // < 5 KB
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
     * @param t contains the printStackTrace
     * @return the stacktrace as string
     */
    public static String getStackTraceAsString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw)); // closing string writer has no effect
        return sw.toString();
    }

    /**
     * Checks is trimmed <code>value</code> is not empty.
     *
     * @param value String to test
     * @return <em>empty</em> if value is <code>null</code> or empty after trimming.
     */
    public static Optional<String> filterTrimmedNotEmpty(String value) {
        return Optional.ofNullable(value)
            .map(String::trim)
            .filter(s -> !s.isEmpty());
    }

    /**
     * Measures the time of a method call.
     * timeHandler is guaranteed to be called even if exception is thrown.
     * @param unit time unit for timeHandler
     * @param timeHandler gets the duration in <code>unit</code>
     * @param task method reference
     * @throws T if task.run() throws Exception
     */
    public static <T extends Throwable> void measure(TimeUnit unit, Consumer<Long> timeHandler,
        MCRThrowableTask<T> task) throws T {
        long time = System.nanoTime();
        try {
            task.run();
        } finally {
            time = System.nanoTime() - time;
            timeHandler.accept(unit.convert(time, TimeUnit.NANOSECONDS));
        }
    }

    /**
     * Measures and logs the time of a method call
     *
     * @param task method reference
     * @throws T if task.run() throws Exception
     */
    public static <T extends Throwable> Duration measure(MCRThrowableTask<T> task) throws T {
        long time = System.nanoTime();
        task.run();
        time = System.nanoTime() - time;
        return Duration.of(time, TimeUnit.NANOSECONDS.toChronoUnit());
    }

    public static Path safeResolve(Path basePath, Path resolve) {
        Path absoluteBasePath = Objects.requireNonNull(basePath)
            .toAbsolutePath();
        final Path resolved = absoluteBasePath
            .resolve(Objects.requireNonNull(resolve))
            .normalize();

        if (resolved.startsWith(absoluteBasePath)) {
            return resolved;
        }
        throw new MCRException("Bad path: " + resolve);
    }

    public static Path safeResolve(Path basePath, String... resolve) {
        if (resolve.length == 0) {
            return basePath;
        }

        String[] more = Stream.of(resolve).skip(1).toArray(String[]::new);
        final Path resolvePath = basePath.getFileSystem().getPath(resolve[0], more);
        return safeResolve(basePath, resolvePath);
    }

    /**
     * Creates a new {@link EnumSet} from a collection of enum values.
     * <p>
     * Intended as an alternative to {@link EnumSet#copyOf(Collection)} which can't handle an empty collection.
     * 
     * @param enumClass The enum class
     * @param values The collection of enum values.
     * @return A {@link EnumSet} containing all given enum values.
     * @param <T> The enum type.
     */
    public static <T extends Enum<T>> Set<T> enumSetOf(Class<T> enumClass, Collection<T> values) {
        Set<T> enumSet = EnumSet.noneOf(enumClass);
        enumSet.addAll(values);
        return enumSet;
    }

}
