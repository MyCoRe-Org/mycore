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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStringContent;

public class MCRHTML2XHTMLContentTransformer extends MCRContentTransformer {

    @Override
    public MCRContent transform(MCRContent source) throws IOException {
        String htmlAsString = source.asString();

        Document document = Jsoup.parse(htmlAsString);
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        document.outputSettings().escapeMode(Entities.EscapeMode.xhtml);

        String s = document.outerHtml().replace("<html>", "<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        MCRStringContent stringContent = new MCRStringContent(s);
        stringContent.setMimeType("application/xhtml+xml");
        stringContent.setName(source.getName() + ".html");
        return stringContent;
    }

    @Override
    public String getFileExtension() throws Exception {
        return "html";
    }

    @Override
    public String getMimeType() throws Exception {
        return "application/xhtml+xml";
    }

}
