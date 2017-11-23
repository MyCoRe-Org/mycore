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

package org.mycore.common.content;

import java.io.IOException;
import java.io.InputStream;

import org.mycore.datamodel.ifs.MCRContentInputStream;

/**
 * Reads MCRContent from an input stream. Typically, this content is not reusable, so that
 * content can only be read once. Please be aware that an instance of this object contains an 
 * open input stream. Thus one has to invoke {@link MCRStreamContent#getInputStream()}.close() when 
 * finished with this object.
 * 
 * @author Frank L\u00FCtzenkichen
 */
public class MCRStreamContent extends MCRContent {

    private InputStream in;

    public MCRStreamContent(InputStream in) {
        if (in == null) {
            throw new NullPointerException("Cannot instantiate MCRStreamContent without InputStream.");
        }
        this.in = in;
    }

    /**
     * @param systemId the systemID of this stream
     */
    public MCRStreamContent(InputStream in, String systemId) {
        this(in);
        setSystemId(systemId);
    }

    /**
     * @param systemId the systemID of this stream
     */
    public MCRStreamContent(InputStream in, String systemId, String docType) {
        this(in, systemId);
        super.docType = docType;
    }

    @Override
    public InputStream getInputStream() {
        return in;
    }

    @Override
    public MCRContentInputStream getContentInputStream() throws IOException {
        if (!(in instanceof MCRContentInputStream))
            in = super.getContentInputStream();
        return (MCRContentInputStream) in;
    }

    /**
     * Returns false, because input streams can only be read once. 
     * Use getReusableCopy() to circumvent this.
     */
    @Override
    public boolean isReusable() {
        return false;
    }
}
