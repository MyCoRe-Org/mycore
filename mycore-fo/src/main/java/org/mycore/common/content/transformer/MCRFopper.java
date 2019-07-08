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

package org.mycore.common.content.transformer;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.TransformerException;

import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.streams.MCRByteArrayOutputStream;
import org.mycore.common.fo.MCRFoFactory;

/**
 * Transforms XSL-FO xml content to PDF. 
 * 
 * @see org.mycore.common.fo.MCRFoFormatterInterface
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRFopper extends MCRContentTransformer {

    @Override
    public MCRContent transform(MCRContent source) throws IOException {
        MCRByteArrayOutputStream pdf = new MCRByteArrayOutputStream(32 * 1024);
        try {
            MCRFoFactory.getFoFormatter().transform(source, pdf);
        } catch (TransformerException e) {
            throw new IOException(e);
        }
        return new MCRByteContent(pdf.getBuffer(), 0, pdf.size(), source.lastModified());
    }

    @Override
    public void transform(MCRContent source, OutputStream out) throws IOException {
        try {
            MCRFoFactory.getFoFormatter().transform(source, out);
        } catch (TransformerException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String getMimeType() {
        return "application/pdf";
    }

    @Override
    public String getFileExtension() {
        return "pdf";
    }
}
