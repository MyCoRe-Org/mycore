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

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileContentInfoFactory;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;

/**
 * Reads MCRContent from Apache Commons Virtual Filesystem (VFS) sources.
 * This includes reading from URLs or URIs.

 * @author Frank L\u00FCtzenkirchen
 */
public class MCRVFSContent extends MCRContent {

    private FileObject fo;

    private static final Logger LOGGER = LogManager.getLogger();

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
        LOGGER.debug(() -> getDebugMessage("{}: returning InputStream of {}", "open"));
        return new FilterInputStream(content.getInputStream()) {
            @Override
            public void close() throws IOException {
                LOGGER.debug(() -> getDebugMessage("{}: closing InputStream of {}", "close"));
                super.close();
                content.close();
            }
        };
    }
    private Message getDebugMessage(String paramMsg, String throwableMsg){
        final String uri = fo.getName().getURI();
        final String id = toString().substring(toString().indexOf('@'));
        return new ParameterizedMessage(paramMsg, new String[] { id, uri }, new IOException(throwableMsg));
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
