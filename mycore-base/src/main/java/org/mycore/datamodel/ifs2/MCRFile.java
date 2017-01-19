/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.datamodel.ifs2;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.streams.MCRDevNull;
import org.mycore.datamodel.ifs.MCRContentInputStream;

/**
 * Represents a file stored in a file collection. This is a file that is
 * imported from outside the system, and may be updated and modified afterwards.
 * 
 * @author Frank Lützenkirchen
 * 
 */
public class MCRFile extends MCRStoredNode {

    private static final MCRDevNull DEV_NULL = new MCRDevNull();

    private final static Logger LOGGER = LogManager.getLogger(MCRFile.class);

    /**
     * The md5 checksum of the empty file
     */
    public final static String MD5_OF_EMPTY_FILE = "d41d8cd98f00b204e9800998ecf8427e";

    /**
     * Returns a MCRFile object representing an existing file already stored in
     * the store.
     * 
     * @param parent
     *            the parent directory containing this file
     * @param fo
     *            the file in the local underlying filesystem storing this file
     */
    protected MCRFile(MCRDirectory parent, FileObject fo, Element data) throws IOException {
        super(parent, fo, data);
    }

    /**
     * Creates a new MCRFile that does not exist yet
     * 
     * @param parent
     *            the parent directory
     * @param name
     *            the file name
     */
    protected MCRFile(MCRDirectory parent, String name) throws IOException {
        super(parent, name, "file");
        fo.createFile();
        data.setAttribute("md5", MCRFile.MD5_OF_EMPTY_FILE);
        getRoot().saveAdditionalData();
    }

    /**
     * Returns a MCRVirtualNode contained in this file as a child. A file that
     * is a container, like zip or tar, may contain other files as children.
     */
    @Override
    protected MCRVirtualNode buildChildNode(FileObject fo) throws IOException {
        return new MCRVirtualNode(this, fo);
    }

    /**
     * Returns the md5 checksum of the file's content.
     * 
     * @return the md5 checksum of the file's content.
     */
    public String getMD5() {
        return data.getAttributeValue("md5");
    }

    /**
     * Returns the file name extension, which is the part after the last dot in
     * the filename.
     * 
     * @return the file extension, or the empty string if the file name does not
     *         have an extension
     */
    public String getExtension() {
        String name = getName();
        int pos = name.lastIndexOf(".");
        return pos == -1 ? "" : name.substring(pos + 1);
    }

    /**
     * Sets the content of this file.
     * 
     * @param source
     *            the content to be read
     * @return the MD5 checksum of the stored content
     */
    public String setContent(MCRContent source) throws IOException {
        MCRContentInputStream cis = source.getContentInputStream();
        source.sendTo(fo);
        String md5 = cis.getMD5String();
        data.setAttribute("md5", md5);
        getRoot().saveAdditionalData();
        return md5;
    }

    /**
     * Repairs additional metadata of this file and all its children
     */
    @Override
    void repairMetadata() throws IOException {
        data.setName("file");
        data.setAttribute("name", getName());
        data.removeChildren("file");
        data.removeChildren("directory");
        MCRContentInputStream cis = getContent().getContentInputStream();
        IOUtils.copy(cis, DEV_NULL);
        cis.close();
        String md5 = cis.getMD5String();
        if (!md5.equals(data.getAttributeValue("md5"))) {
            LOGGER.warn("Fixed MD5 of " + getPath() + " to " + md5);
            data.setAttribute("md5", md5);
        }
    }
}
