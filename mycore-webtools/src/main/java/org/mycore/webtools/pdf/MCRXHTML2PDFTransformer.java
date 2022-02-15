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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;

import org.mycore.common.MCRClassTools;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRContentTransformer;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.layout.font.FontProvider;

/**
 * Takes XHTML as input and transforms it to a PDF file
 */
public class MCRXHTML2PDFTransformer extends MCRContentTransformer {

    private String id;

    protected FontProvider fontProvider;

    @Override
    public void init(String id) {
        this.id = id;
        super.init(id);
    }

    @Override
    public MCRContent transform(MCRContent source) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String s = source.asString();
        s = Normalizer.normalize(s, Normalizer.Form.NFC); // required because some data is not normalized
        s = s.replace(" ", ""); // required because itext cant handle these

        ConverterProperties converterProperties = new ConverterProperties();
        converterProperties.setFontProvider(lazyInitFontProvider());
        HtmlConverter.convertToPdf(s, baos, converterProperties);

        return new MCRByteContent(baos.toByteArray());
    }

    protected synchronized FontProvider lazyInitFontProvider() {
        if (fontProvider == null) {
            fontProvider = new FontProvider();
            final ClassLoader cl = MCRClassTools.getClassLoader();
            MCRConfiguration2.getString("MCR.ContentTransformer." + id + ".FontResources")
                .stream()
                .flatMap(MCRConfiguration2::splitValue)
                .forEach((path) -> {
                    try (InputStream is = cl.getResource(path).openStream()) {
                        fontProvider.addFont(is.readAllBytes());
                    } catch (IOException e) {
                        throw new MCRConfigurationException("Error while loading configured fonts!", e);
                    }
                });
        }

        return fontProvider;
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
