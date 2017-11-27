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

package org.mycore.datamodel.niofs.ifs1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.niofs.MCRFileAttributes;

abstract class MCRBasicFileAttributeViewImpl implements BasicFileAttributeView {
    private static Logger LOGGER = LogManager.getLogger(MCRBasicFileAttributeViewImpl.class);

    public MCRBasicFileAttributeViewImpl() {
        super();
    }

    static MCRFileAttributes<String> readAttributes(MCRFilesystemNode node) throws IOException {
        return node.getBasicFileAttributes();
    }

    @Override
    public String name() {
        return "basic";
    }

    @Override
    public MCRFileAttributes<String> readAttributes() throws IOException {
        MCRFilesystemNode node = resolveNode();
        return readAttributes(node);
    }

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
        MCRFilesystemNode node = resolveNode();
        if (node instanceof MCRFile) {
            MCRFile file = (MCRFile) node;
            file.adjustMetadata(lastModifiedTime, file.getMD5(), file.getSize());
            Files.getFileAttributeView(file.getLocalFile().toPath(), BasicFileAttributeView.class).setTimes(
                lastModifiedTime,
                lastAccessTime, createTime);
        } else if (node instanceof MCRDirectory) {
            LOGGER.warn("Setting times on directories is not supported: {}", node.toPath());
        }
    }

    protected abstract MCRFilesystemNode resolveNode() throws IOException;

}
