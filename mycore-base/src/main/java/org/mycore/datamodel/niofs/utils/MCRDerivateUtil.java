package org.mycore.datamodel.niofs.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.niofs.MCRPath;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class MCRDerivateUtil {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Renames multiple files in one Derivate with the given pattern. You can try out your pattern with the method
     * {@link #testRenameFile(String, String, String) testRenameFile}.
     *
     * @param derivate the Derivate ID as String
     * @param pattern the RegEx pattern to find the wanted files
     * @param replacement the new name for the files
     * @return a Hashmap with the old name and the new name
     * @throws IOException
     */
    public static Map<String, String> renameFiles(String derivate, String pattern, String replacement)
        throws IOException {
        MCRPath derivateRoot = MCRPath.getPath(derivate, "/");
        Pattern patternObj = Pattern.compile(pattern);
        Map<String, String> resultMap = new HashMap<>();
        Files.walkFileTree(derivateRoot, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
                Matcher matcher = patternObj.matcher(file.getFileName().toString());
                if (matcher.matches()) {
                    LOGGER.debug("The file {} matches the pattern {}", file, pattern);
                    String newFilename;
                    try {
                        newFilename = matcher.replaceAll(replacement);
                    } catch (IndexOutOfBoundsException e) {
                        LOGGER.error("The file {} can't be renamed to {}. To many groups!", file, replacement, e);
                        return FileVisitResult.CONTINUE;
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("The new name '{}' contains illegal characters!", replacement, e);
                        return FileVisitResult.CONTINUE;
                    }
                    Files.move(file, file.resolveSibling(newFilename));
                    LOGGER.info("The file {} was renamed to {}", file, newFilename);
                    resultMap.put(file.getFileName().toString(), newFilename);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return resultMap;
    }

    /**
     * Tests the rename pattern on one file, so you can try the rename befor renaming all files. This method does not
     * change any files.
     *
     * @param filename a filename to try the pattern on
     * @param pattern the RegEx pattern to find the wanted files
     * @param replacement the new name for the files
     * @return the new filename
     */
    public static String testRenameFile(String filename, String pattern, String replacement) {
        String newFilename = "";
        try {
            Pattern patternObj = Pattern.compile(pattern);
            Matcher matcher = patternObj.matcher(filename);
            newFilename = matcher.replaceAll(replacement);
            LOGGER.info("The file {} will be renamed to {}", filename, newFilename);
        } catch (PatternSyntaxException e) {
            LOGGER.error("The pattern '{}' contains errors!", pattern, e);
        } catch (IndexOutOfBoundsException e) {
            LOGGER.error("The file {} can't be renamed to {}. To many groups!", filename, replacement, e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("The new name '{}' contains illegal characters!", replacement, e);
        }
        return newFilename;
    }

}
