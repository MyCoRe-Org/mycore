package org.mycore.frontend;

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

public class MCRDerivateUtil {

    static Logger LOGGER = LogManager.getLogger(MCRDerivateUtil.class);

    public static Map<String, String> renameFiles(String derivate, String pattern, String newName)
        throws IOException {
        MCRPath derivateRoot = MCRPath.getPath(derivate, "/");
        Pattern patternObj = Pattern.compile(pattern);
        Map<String, String> resultMap = new HashMap<String, String>();
        Files.walkFileTree(derivateRoot, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
                Matcher matcher = patternObj.matcher(file.getFileName().toString());
                if (matcher.matches()) {
                    LOGGER.info("The file " + file + " matches the pattern " + pattern);
                    String newFilename;
                    try {
                        newFilename = matcher.replaceAll(newName);
                    }
                    catch (IndexOutOfBoundsException e) {
                        LOGGER.info("The file " + file + " can't be renamed to " + newName + ". To many groups!");
                        return FileVisitResult.CONTINUE;
                    }
                    Files.move(file, file.resolveSibling(newFilename));
                    LOGGER.info("The file " + file + " was renamed to " + newFilename);
                    resultMap.put(file.getFileName().toString(), newFilename);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return resultMap;
    }

    public static String testRenameFile(String filename, String pattern, String newName)
        throws IOException {
        Pattern patternObj = Pattern.compile(pattern);
        Matcher matcher = patternObj.matcher(filename);
        String newFilename;
        try {
            newFilename = matcher.replaceAll(newName);
            LOGGER.info("The file " + filename + " will be renamed to " + newFilename);
        }
        catch (IndexOutOfBoundsException e) {
            LOGGER.info("The file " + filename + " can't be renamed to " + newName + ". To many groups!");
        }
        return newName;
    }
}
