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

package org.mycore.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper class for tests.
 */
public class MCRTestHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Deletes the given paths recursively.
     *
     * @param paths the paths to delete
     * @throws IOException if a file or directory could not be deleted
     */
    public static void deleteRecursively(Path... paths) throws IOException {
        //use deleteRecursively(Path path) method and ensure that all exceptions are thrown as suppressed exceptions
        List<Exception> suppressed = new ArrayList<>();
        for (Path path : paths) {
            try {
                deleteRecursively(path);
            } catch (Exception e) {
                suppressed.add(e);
            }
        }
        if (!suppressed.isEmpty()) {
            if (suppressed.size() == 1 && suppressed.getFirst() instanceof IOException ioe) {
                throw ioe;
            }
            IOException mainIOException = new IOException("Error deleting: " + Arrays.toString(paths));
            suppressed.forEach(mainIOException::addSuppressed);
            throw mainIOException;
        }
    }

    /**
     * Deletes the given path recursively.
     *
     * @param path the path to delete
     * @throws IOException if a file or directory could not be deleted
     */
    private static void deleteRecursively(Path path) throws IOException {

        if (path == null || !Files.exists(path)) {
            return;
        }

        List<IOException> suppressed = new ArrayList<>();
        try (Stream<Path> pathStream = Files.walk(path)) {
            pathStream
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        LOGGER.debug(() -> "Deleting: " + p);
                        Files.delete(p);
                    } catch (IOException e) {
                        suppressed.add(e);
                    }
                });
        } catch (IOException e) {
            suppressed.add(e);
        }
        if (!suppressed.isEmpty()) {
            if (suppressed.size() == 1) {
                throw suppressed.getFirst();
            }
            IOException mainIOException = new IOException("Error deleting: " + path);
            suppressed.forEach(mainIOException::addSuppressed);
            throw mainIOException;
        }
    }

}
