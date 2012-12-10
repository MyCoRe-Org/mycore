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

import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.VFS;
import org.apache.log4j.Logger;

/**
 * Reads MCRContent from Apache Commons Virtual Filesystem (VFS) sources.
 * This includes reading from URLs or URIs.

 * @author Frank L\u00FCtzenkirchen
 */
public class MCRVFSContent extends MCRContent {

    private FileObject fo;

    public MCRVFSContent(FileObject fo) throws IOException {
        this.fo = fo;
        setSystemId(fo.getURL().toString());
    }

    public MCRVFSContent(URL url) throws IOException {
        this(VFS.getManager().resolveFile(url.toExternalForm()));
    }

    public MCRVFSContent(URI uri) throws IOException {
        this(uri.toURL());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        final FileContent content = fo.getContent();
        final String uri = fo.getName().getURI();
        final Logger logger = Logger.getLogger(MCRVFSContent.class);
        final String id = toString().substring(toString().indexOf('@'));
        logger.info(id + ": returning InputStream of " + uri, new IOException("open"));
        return new FilterInputStream(content.getInputStream()) {
            @Override
            public void close() throws IOException {
                logger.info(id + ": closing Inputstream of " + uri, new IOException("close"));
                super.close();
                content.close();
            }
        };
    }
}
