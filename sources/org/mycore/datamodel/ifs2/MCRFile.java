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

import java.io.File;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.provider.local.LocalFile;
import org.jdom.Element;
import org.mycore.common.MCRUtils;

/**
 * Represents a file stored in a file collection. This is a file that is
 * imported from outside the system, and may be updated and modified afterwards.
 * 
 * @author Frank Lützenkirchen
 * 
 */
public class MCRFile extends MCRStoredNode {
    /**
     * The md5 checksum of the file's contents.
     */
    protected String md5;

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
    protected MCRFile(MCRDirectory parent, FileObject fo) throws Exception {
        super(parent, fo);
        parent.readChildData(this);
    }

    /**
     * Creates a new MCRFile and stores it in the underlying local filesystem.
     * 
     * @param parent
     *            the parent directory containing this file
     * @param name
     *            the name of the file to be created and stored
     */
    public MCRFile(MCRDirectory parent, String name) throws Exception {
        super(parent, VFS.getManager().resolveFile(parent.fo, name));
        md5 = MD5_OF_EMPTY_FILE;
        fo.createFile();
        updateMetadata();
    }

    /**
     * Returns a MCRVirtualNode contained in this file as a child. A file that
     * is a container, like zip or tar, may contain other files as children.
     */
    protected MCRVirtualNode buildChildNode(FileObject fo) throws Exception {
        return new MCRVirtualNode(this, fo);
    }

    /**
     * Writes all metadata of this file to the given XML element
     */
    protected void writeChildData(Element entry) throws Exception {
        super.writeChildData(entry);
        entry.setAttribute("md5", this.getMD5());
        entry.setAttribute("size", String.valueOf(this.getSize()));
    }

    /**
     * Reads metadata of this file from the given XML element and stores it in
     * this object itself.
     */
    protected void readChildData(Element entry) throws Exception {
        super.readChildData(entry);
        md5 = entry.getAttributeValue("md5");
    }

    /**
     * Repairs the metadata of this file by rebuilding it from the underlying
     * local filesystem. This includes recreation of the md5 checksum. Stores
     * the metadata rebuilt in the parent directory.
     */
    public void repairMetadata() throws Exception {
        MCRContent content = new MCRContent( fo );
        MCRContentInputStream cis = content.getContentInputStream();
        MCRUtils.copyStream(cis, null);
        cis.close();
        md5 = cis.getMD5String();
        updateMetadata();
    }

    /**
     * Deletes this file and its content from the store. This object is illegal
     * afterwards and must not be used any more.
     */
    public void delete() throws Exception {
        super.delete();
        fo.delete();
    }

    /**
     * Returns the md5 checksum of the file's content.
     * 
     * @return the md5 checksum of the file's content.
     */
    public String getMD5() {
        return md5;
    }

    /**
     * Returns the file name extension, which is the part after the last dot in
     * the filename.
     * 
     * @return the file extension, or the empty string if the file name does not
     *         have an extension
     */
    public String getExtension() {
        String name = this.getName();
        int pos = name.lastIndexOf(".");
        return (pos == -1 ? "" : name.substring(pos + 1));
    }

    /**
     * Sets the content of this file.
     * 
     * @param content
     *            the content to be read
     * @return the MD5 checksum of the stored content
     */
    public String setContent(MCRContent source) throws Exception {
        MCRContentInputStream cis = source.getContentInputStream();
        source.sendTo(fo);
        md5 = cis.getMD5String();
        updateMetadata();
        return md5;
    }

    /**
     * Returns the local java.io.File representing this stored file. Be careful
     * to use this only for reading data, do never modify directly!
     * 
     * @return the file in the local filesystem representing this file
     */
    public File getLocalFile() throws Exception {
        if (fo instanceof LocalFile)
            return new File(fo.getURL().getPath());
        else
            return null;
    }
}
