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

package org.mycore.datamodel.niofs.ifs2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;

import org.mycore.datamodel.ifs2.MCRStoredNode;
import org.mycore.datamodel.niofs.MCRFileAttributes;

abstract class MCRBasicFileAttributeViewImpl implements BasicFileAttributeView {

    MCRBasicFileAttributeViewImpl() {
        super();
    }

    static MCRFileAttributes<String> readAttributes(MCRStoredNode node) throws IOException {
        return node.getBasicFileAttributes();
    }

    @Override
    public String name() {
        return "basic";
    }

    @Override
    public MCRFileAttributes<String> readAttributes() throws IOException {
        MCRStoredNode node = resolveNode();
        return readAttributes(node);
    }

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
        MCRStoredNode node = resolveNode();
        BasicFileAttributeView localView = Files
            .getFileAttributeView(node.getLocalPath(), BasicFileAttributeView.class);
        localView.setTimes(lastModifiedTime, lastAccessTime, createTime);
    }

    protected abstract MCRStoredNode resolveNode() throws IOException;

}
