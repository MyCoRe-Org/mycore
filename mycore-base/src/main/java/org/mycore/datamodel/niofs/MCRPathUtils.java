/**
 * 
 */
package org.mycore.datamodel.niofs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public abstract class MCRPathUtils {

    private MCRPathUtils() {

    }

    /**
     * Returns requested {@link BasicFileAttributes} or null if file does not exist.
     * 
     * Same as {@link Files#readAttributes(Path, Class, LinkOption...)} without throwing {@link IOException}.
     * 
     * @param   path
     *          the path to the file
     * @param   type
     *          the {@code Class} of the file attributes required
     *          to read
     * @param   options
     *          options indicating how symbolic links are handled
     *
     * @return  the file attributes
     */
    public static <A extends BasicFileAttributes> A getAttributes(Path path, Class<A> type, LinkOption... options) {
        try {
            return Files.readAttributes(path, type, options);
        } catch (NoSuchFileException | FileNotFoundException e) {
            //we expect that file may not exist
        } catch (IOException e) {
            //any other IOException is catched
            LogManager.getLogger(MCRPathUtils.class).info("Error while retrieving attributes of file: " + path, e);
        }
        return null;
    }

    public static Path getPath(FileSystem targetFS, String fileName) {
        String[] nameComps = fileName.replace('\\', '/').split("/");
        if (nameComps.length == 1) {
            return targetFS.getPath(nameComps[0]);
        } else {
            return targetFS.getPath(nameComps[0], Arrays.copyOfRange(nameComps, 1, nameComps.length));
        }

    }

}
