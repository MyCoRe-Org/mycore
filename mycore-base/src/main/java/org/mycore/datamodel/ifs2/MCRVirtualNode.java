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

package org.mycore.datamodel.ifs2;

import java.io.IOException;

import org.apache.commons.vfs2.FileObject;

/**
 * A virtual node in a file collection, which may be a child node of a container
 * file type like zip or tar. Such files can be browsed and read using this node
 * type.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRVirtualNode extends MCRNode {
    /**
     * Creates a new virtual node
     * 
     * @param parent
     *            the parent node containing this node
     * @param fo
     *            the file object in Apache VFS representing this node
     */
    protected MCRVirtualNode(MCRNode parent, FileObject fo) {
        super(parent, fo);
    }

    /**
     * Returns a virtual node that is a child of this virtual node.
     */
    @Override
    protected MCRVirtualNode buildChildNode(FileObject fo) throws IOException {
        return new MCRVirtualNode(this, fo);
    }
}
