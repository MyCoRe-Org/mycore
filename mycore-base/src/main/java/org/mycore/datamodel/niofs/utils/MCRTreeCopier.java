/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.datamodel.niofs.utils;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simple {@link FileVisitor} that recursive copies a directory
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRTreeCopier implements FileVisitor<Path> {
    private static final Logger LOGGER = LogManager.getLogger(MCRTreeCopier.class);

    private final Path source;

    private final Path target;

    private final boolean renameExisting;

    public MCRTreeCopier(Path source, Path target) throws NoSuchFileException {
        this(source, target, false);
    }

    public MCRTreeCopier(Path source, Path target, boolean renameOnExisting) throws NoSuchFileException {
        this.renameExisting = renameOnExisting;
        if (Files.notExists(target)) {
            throw new NoSuchFileException(target.toString(), null, "Target directory does not exist.");
        }
        this.source = source;
        this.target = target;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        Path newdir = target.resolve(toTargetFS(source.relativize(dir)));
        try {
            Files.copy(dir, newdir, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (FileAlreadyExistsException x) {
            // (okay if directory already exists).
            // ignore
        } catch (IOException x) {
            LOGGER.error("Unable to create: {}", newdir, x);
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }

    private void copyFile(Path source, Path target) {
        try {
            if (renameExisting && Files.exists(target)) {
                int nameTry = 1;
                String fileName = target.getFileName().toString();
                int numberPosition = fileName.lastIndexOf(".") == -1 ? fileName.length() : fileName.lastIndexOf(".");
                String prefixString = fileName.substring(0, numberPosition);
                String suffixString = fileName.substring(numberPosition, fileName.length());
                String newName = null;
                Path parent = target.getParent();
                do {
                    newName = prefixString + nameTry++ + suffixString;
                } while (Files.exists(target = parent.resolve(newName)));
            }
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException x) {
            LOGGER.error("Unable to copy: {}", source, x);
        }
    }

    private Path toTargetFS(Path source) {
        if (target.getFileSystem().equals(source.getFileSystem())) {
            return source;
        }
        String[] nameParts = new String[source.getNameCount() - 1];
        for (int i = 0; i < nameParts.length; i++) {
            nameParts[i] = source.getName(i + 1).toString();
        }
        return target.getFileSystem().getPath(source.getName(0).toString(), nameParts);
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        // fix up modification time of directory when done
        if (exc == null) {
            Path newdir = target.resolve(toTargetFS(source.relativize(dir)));
            try {
                FileTime time = Files.getLastModifiedTime(dir);
                Files.setLastModifiedTime(newdir, time);
            } catch (IOException x) {
                LOGGER.error("Unable to copy all attributes to: {}", newdir, x);
            }
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        copyFile(file, target.resolve(toTargetFS(source.relativize(file))));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        if (exc instanceof FileSystemLoopException) {
            LOGGER.error("cycle detected: {}", file);
        } else {
            LOGGER.error("Unable to copy: {}", file, exc);
        }
        return FileVisitResult.CONTINUE;
    }
}
