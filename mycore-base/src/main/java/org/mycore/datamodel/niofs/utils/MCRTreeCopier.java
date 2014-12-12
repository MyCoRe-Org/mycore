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

import org.apache.log4j.Logger;

/**
 * Simple {@link FileVisitor} that recursive copies a directory
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRTreeCopier implements FileVisitor<Path> {
    private static final Logger LOGGER = Logger.getLogger(MCRTreeCopier.class);

    private final Path source;

    private final Path target;

    public MCRTreeCopier(Path source, Path target) throws NoSuchFileException {
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
            LOGGER.error("Unable to create: " + newdir, x);
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        copyFile(file, target.resolve(toTargetFS(source.relativize(file))));
        return FileVisitResult.CONTINUE;
    }

    private void copyFile(Path source, Path target) {
        try {
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException x) {
            LOGGER.error("Unable to copy: " + source, x);
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
                LOGGER.error("Unable to copy all attributes to: " + newdir, x);
            }
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        if (exc instanceof FileSystemLoopException) {
            LOGGER.error("cycle detected: " + file);
        } else {
            LOGGER.error("Unable to copy: " + file, exc);
        }
        return FileVisitResult.CONTINUE;
    }
}