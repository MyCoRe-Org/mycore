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

package org.mycore.webtools.pdf;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRContentTransformer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Takes XHTML as input and transforms it to a PDF file
 */
public class MCRXHTML2PDFTransformer extends MCRContentTransformer {

    @Override
    public MCRContent transform(MCRContent source) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (InputStream xhtmlIS = source.getInputStream()) {
            ConverterProperties converterProperties = new ConverterProperties();
            HtmlConverter.convertToPdf(xhtmlIS, baos, converterProperties);
        }

        return new MCRByteContent(baos.toByteArray());
    }

    @Override
    public String getFileExtension() throws Exception {
        return "pdf";
    }

    @Override
    public String getMimeType() throws Exception {
        return "application/pdf";
    }
}
