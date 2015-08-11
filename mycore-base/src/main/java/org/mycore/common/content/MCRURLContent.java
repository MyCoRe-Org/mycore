/*
 * $Id$
 * $Revision: 5697 $ $Date: Nov 16, 2012 $
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FilenameUtils;
import org.xml.sax.InputSource;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRURLContent extends MCRContent {

    private URL url;

    public MCRURLContent(URL url) {
        super();
        this.url = url;
        this.setSystemId(url.toString());
        String fileName = url.getPath();
        if (fileName.endsWith("/")) {
            fileName = FilenameUtils.getPathNoEndSeparator(fileName); //removes final '/';
        }
        setName(FilenameUtils.getName(fileName));
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return url.openStream();
    }

    @Override
    public Source getSource() throws IOException {
        return new StreamSource(getSystemId());
    }

    @Override
    public InputSource getInputSource() throws IOException {
        return new InputSource(getSystemId());
    }

    @Override
    public long length() throws IOException {
        return url.openConnection().getContentLengthLong();
    }

    @Override
    public long lastModified() throws IOException {
        return url.openConnection().getLastModified();
    }

    @Override
    public String getETag() throws IOException {
        URLConnection openConnection = url.openConnection();
        openConnection.connect();
        String eTag = openConnection.getHeaderField("ETag");
        if (eTag != null) {
            return eTag;
        }
        long lastModified = openConnection.getLastModified();
        long length = openConnection.getContentLengthLong();
        eTag = getSimpleWeakETag(url.toString(), length, lastModified);
        return eTag == null ? null : eTag.substring(2);
    }

    @Override
    public String getMimeType() throws IOException {
        return super.getMimeType() == null ? url.openConnection().getContentType() : super.getMimeType();
    }

}
