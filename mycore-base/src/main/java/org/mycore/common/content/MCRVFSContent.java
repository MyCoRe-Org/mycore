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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileContentInfoFactory;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.apache.log4j.Logger;

/**
 * Reads MCRContent from Apache Commons Virtual Filesystem (VFS) sources.
 * This includes reading from URLs or URIs.

 * @author Frank L\u00FCtzenkirchen
 */
public class MCRVFSContent extends MCRContent {

    private FileObject fo;

    private static final Logger LOGGER = Logger.getLogger(MCRVFSContent.class);

    public MCRVFSContent(FileObject fo) throws IOException {
        this.fo = fo;
        setName(fo.getName().getBaseName());
        setSystemId(fo.getURL().toString());
    }

    public MCRVFSContent(URL url) throws IOException {
        this(VFS.getManager().resolveFile(url.toExternalForm()));
    }

    public MCRVFSContent(URI uri) throws IOException {
        this(uri.toURL());
    }

    public MCRVFSContent(FileObject fo, String docType) throws IOException {
        this(fo);
        super.docType = docType;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        final FileContent content = fo.getContent();
        if (LOGGER.isDebugEnabled()) {
            final String uri = fo.getName().getURI();
            final String id = toString().substring(toString().indexOf('@'));
            LOGGER.debug(id + ": returning InputStream of " + uri, new IOException("open"));
            return new FilterInputStream(content.getInputStream()) {
                @Override
                public void close() throws IOException {
                    LOGGER.debug(id + ": closing Inputstream of " + uri, new IOException("close"));
                    super.close();
                    content.close();
                }
            };
        }
        return new FilterInputStream(content.getInputStream()) {
            @Override
            public void close() throws IOException {
                super.close();
                content.close();
            }
        };
    }

    @Override
    public long length() throws IOException {
        FileContent content = fo.getContent();
        try {
            return content.getSize();
        } finally {
            fo.close();
        }
    }

    @Override
    public long lastModified() throws IOException {
        FileContent content = fo.getContent();
        try {
            return content.getLastModifiedTime();
        } finally {
            fo.close();
        }
    }

    @Override
    public String getETag() throws IOException {
        FileContent content = fo.getContent();
        try {
            String systemId = getSystemId();
            if (systemId == null) {
                systemId = fo.getName().getURI();
            }
            long length = content.getSize();
            long lastModified = content.getLastModifiedTime();
            String eTag = getSimpleWeakETag(systemId, length, lastModified);
            if (eTag == null) {
                return null;
            }
            return eTag.substring(2); //remove weak
        } finally {
            content.close();
        }
    }

    @Override
    public String getMimeType() throws IOException {
        if (super.getMimeType() != null) {
            return super.getMimeType();
        }
        FileContentInfoFactory fileContentInfoFactory = fo.getFileSystem().getFileSystemManager()
            .getFileContentInfoFactory();
        FileContent content = fo.getContent();
        try {
            return fileContentInfoFactory.create(content).getContentType();
        } finally {
            content.close();
        }
    }
}
