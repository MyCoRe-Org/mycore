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

import java.io.IOException;
import java.io.InputStream;

import org.mycore.datamodel.ifs.MCRContentInputStream;;

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
