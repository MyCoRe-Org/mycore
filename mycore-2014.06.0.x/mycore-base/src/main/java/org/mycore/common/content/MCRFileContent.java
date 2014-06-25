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

package org.mycore.common.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * Reads MCRContent from a local file.
 * 
 * @author Frank L\u00FCtzenkichen
 */
public class MCRFileContent extends MCRContent {

    private File file;

    public MCRFileContent(File file) throws IOException {
        this.file = file;
        setSystemId(file.toURI().toString());
    }

    public MCRFileContent(String file) throws IOException {
        this(new File(file));
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public long length() throws IOException {
        return file.length();
    }

    @Override
    public long lastModified() throws IOException {
        return file.lastModified();
    }

    @Override
    public String getETag() throws IOException {
        String eTag = getSimpleWeakETag(file.toURI().toString(), length(), lastModified());
        return eTag == null ? null : eTag.substring(2);
    }

    @Override
    public ReadableByteChannel getReadableByteChannel() throws IOException {
        return FileChannel.open(file.toPath(), StandardOpenOption.READ);
    }

    @Override
    public String getMimeType() throws IOException {
        return super.getMimeType() == null ? Files.probeContentType(file.toPath()) : super.getMimeType();
    }
}
