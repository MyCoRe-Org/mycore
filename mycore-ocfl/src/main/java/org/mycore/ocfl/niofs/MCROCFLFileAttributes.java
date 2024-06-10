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

package org.mycore.ocfl.niofs;

import java.io.IOException;
import java.nio.file.attribute.FileTime;

import org.mycore.common.digest.MCRDigest;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRVersionedPath;

public class MCROCFLFileAttributes implements MCRFileAttributes<Object> {

    private final FileTime creationTime;

    private final FileTime modifiedTime;

    private final FileTime accessTime;

    private final boolean file;

    private final boolean directory;

    private final long size;

    private final MCRDigest digest;

    private final Object fileKey;

    public MCROCFLFileAttributes(MCROCFLVirtualObject virtualObject, MCRVersionedPath path) throws IOException {
        this.creationTime = virtualObject.getCreationTime(path);
        this.modifiedTime = virtualObject.getModifiedTime(path);
        this.accessTime = virtualObject.getAccessTime(path);
        this.file = virtualObject.isFile(path);
        this.directory = virtualObject.isDirectory(path);
        this.size = virtualObject.getSize(path);
        this.digest = virtualObject.getDigest(path);
        this.fileKey = virtualObject.getFileKey(path);
    }

    @Override
    public FileTime creationTime() {
        return this.creationTime;
    }

    @Override
    public FileTime lastModifiedTime() {
        return this.modifiedTime;
    }

    @Override
    public FileTime lastAccessTime() {
        return this.accessTime;
    }

    @Override
    public boolean isRegularFile() {
        return this.file;
    }

    @Override
    public boolean isDirectory() {
        return this.directory;
    }

    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    @Override
    public boolean isOther() {
        return false;
    }

    @Override
    public long size() {
        return this.size;
    }

    @Override
    public Object fileKey() {
        return this.fileKey;
    }

    @Override
    public MCRDigest digest() {
        return this.digest;
    }

}
