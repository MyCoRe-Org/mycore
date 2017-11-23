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
