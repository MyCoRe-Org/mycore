package org.mycore.datamodel.niofs.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A {@link FileVisitor} to delete a directory recursivly
 * <pre>
 *   Files.walkFileTree(rootPath, MCRRecursiveDeleter.instance())
 * </pre>
 * @author Thomas Scheffler (yagee)
 *
 */
public final class MCRRecursiveDeleter extends SimpleFileVisitor<Path> {

    private static final MCRRecursiveDeleter instance = new MCRRecursiveDeleter();

    private MCRRecursiveDeleter() {
    }

    public static MCRRecursiveDeleter instance() {
        return instance;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return super.visitFile(file, attrs);
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc != null) {
            throw exc;
        }
        if (dir.getNameCount() > 0) {
            Files.delete(dir);
        }
        return super.postVisitDirectory(dir, exc);
    }
}
