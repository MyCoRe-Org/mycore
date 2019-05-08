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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.IOUtils;
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

    private static final Logger LOGGER = LogManager.getLogger(MCRFile.class);

    /**
     * The md5 checksum of the empty file
     */
    public static final String MD5_OF_EMPTY_FILE = "d41d8cd98f00b204e9800998ecf8427e";

    /**
     * Returns a MCRFile object representing an existing file already stored in
     * the store.
     * 
     * @param parent
     *            the parent directory containing this file
     * @param fo
     *            the file in the local underlying filesystem storing this file
     */
    protected MCRFile(MCRDirectory parent, Path fo, Element data) throws IOException {
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
        Files.createFile(path);
        writeData(e -> e.setAttribute("md5", MCRFile.MD5_OF_EMPTY_FILE));
        getRoot().saveAdditionalData();
    }

    /**
     * Returns a MCRVirtualNode contained in this file as a child. A file that
     * is a container, like zip or tar, may contain other files as children.
     */
    @Override
    protected MCRVirtualNode buildChildNode(Path fo) {
        throw new UnsupportedOperationException("not yet implemented");
        //return new MCRVirtualNode(this, path);
    }

    /**
     * Returns the md5 checksum of the file's content.
     * 
     * @return the md5 checksum of the file's content.
     */
    public String getMD5() {
        return readData(e -> e.getAttributeValue("md5"));
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
		try (MCRContentInputStream cis = source.getContentInputStream()) {
			source.sendTo(path, StandardCopyOption.REPLACE_EXISTING);
			String md5 = cis.getMD5String();
			writeData(e -> e.setAttribute("md5", md5));
			getRoot().saveAdditionalData();
			return md5;
		}
    }

    /**
     * updates the MD5 sum of this file to the given value.
     *
     * Use only if you modified the content outside of {@link #setContent(MCRContent)}.
     * @param md5
     * @throws IOException
     */
    public void setMD5(String md5) throws IOException {
        writeData(e -> e.setAttribute("md5", md5));
        getRoot().saveAdditionalData();
    }

    /**
     * Repairs additional metadata of this file and all its children
     */
    @Override
    void repairMetadata() throws IOException {
        MCRContentInputStream cis = getContent().getContentInputStream();
        IOUtils.copy(cis, DEV_NULL);
        cis.close();
        String path = getPath();
        writeData(e -> {
            e.setName("file");
            e.setAttribute("name", getName());
            e.removeChildren("file");
            e.removeChildren("directory");
            String md5 = cis.getMD5String();
            if (!md5.equals(e.getAttributeValue("md5"))) {
                LOGGER.warn("Fixed MD5 of {} to {}", path, md5);
                e.setAttribute("md5", md5);
            }
        });
    }
}
