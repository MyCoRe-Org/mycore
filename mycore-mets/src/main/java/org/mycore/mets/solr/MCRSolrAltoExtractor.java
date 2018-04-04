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

package org.mycore.mets.solr;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.solr.common.SolrInputDocument;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.solr.index.file.MCRSolrFileIndexAccumulator;

/**
 * Extract content and word coordinates of ALTO XML and adds it to the alto_words and alto_content field.
 *
 * @author Matthias Eichner
 */
public class MCRSolrAltoExtractor implements MCRSolrFileIndexAccumulator {

    static XPathExpression<Element> STRING_EXP;

    static {
        String exp = "alto:Layout/alto:Page/alto:PrintSpace//alto:String";
        STRING_EXP = XPathFactory.instance().compile(exp, Filters.element(), null, MCRConstants.ALTO_NAMESPACE);
    }

    @Override
    public void accumulate(SolrInputDocument document, Path filePath, BasicFileAttributes attributes)
            throws IOException {
        String parentPath = MCRPath.toMCRPath(filePath).getParent().getOwnerRelativePath();
        if (!"/alto".equals(parentPath)) {
            return;
        }
        try (InputStream is = Files.newInputStream(filePath)) {
            SAXBuilder builder = new SAXBuilder();
            Document altoDocument = builder.build(is);
            extract(altoDocument.getRootElement(), document);
        } catch (JDOMException e) {
            LogManager.getLogger().error("Unable to parse {}", filePath, e);
        }
    }

    private void extract(Element root, SolrInputDocument document) {
        StringBuilder altoContent = new StringBuilder();
        for (Element stringElement : STRING_EXP.evaluate(root)) {
            String content = stringElement.getAttributeValue("CONTENT");
            String hpos = stringElement.getAttributeValue("HPOS");
            String vpos = stringElement.getAttributeValue("VPOS");
            String width = stringElement.getAttributeValue("WIDTH");
            String height = stringElement.getAttributeValue("HEIGHT");
            if (hpos == null || vpos == null || width == null || height == null) {
                continue;
            }
            String regEx = "\\.0";
            String altoWord = String.join("|", content, hpos.replaceAll(regEx, ""), vpos.replaceAll(regEx, ""),
                    width.replaceAll(regEx, ""), height.replaceAll(regEx, ""));
            altoContent.append(content).append(' ');
            document.addField("alto_words", altoWord);
        }
        document.addField("alto_content", altoContent.toString().trim());
    }

}
