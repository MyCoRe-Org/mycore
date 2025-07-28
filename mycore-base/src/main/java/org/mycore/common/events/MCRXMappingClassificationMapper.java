/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
package org.mycore.common.events;

import java.util.stream.Stream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.events.MCRClassificationMappingEventHandler.Mapper;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;

/**
 * A {@link MCRXMappingClassificationMapper} is a {@link Mapper} that looks for mapping
 * information in all classification values already present in the metadata document.
 * <p>
 * For each classification value, if the corresponding classification category contains a <code>x-mapping</code>
 * label, the content of that label is used as a space separated list of classification category IDs.
 * </p>
 * Example form <code>foo_bar</code>:
 * <pre><code>
 * &lt;category ID=&quot;article&quot;&gt;
 * &nbsp;&lt;label xml:lang=&quot;en&quot; text=&quot;ScolarlyArticle&quot; /&gt;
 * &nbsp;&lt;label xml:lang=&quot;x-mapping&quot; text=&quot;foo_baz:Article&quot; /&gt;
 * &lt;/category&gt;
 * </code></pre>
 * If a metadata document contains the classification category <code>foo_bar:article</code>, the
 * classification category <code>foo_baz:ScolarlyArticle</code> will be provided.
 * <p>
 * No configuration options are available.
 * <p>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.common.events.MCRXMappingClassificationMapper
 * </code></pre>
 */
public class MCRXMappingClassificationMapper
    extends MCRXMappingClassificationMapperBase<Document, MCRCategoryID>
    implements Mapper {

    private static final XPathExpression<Element> CLASSIFICATION_ELEMENT_XPATH;

    static {
        CLASSIFICATION_ELEMENT_XPATH = XPathFactory.instance().compile("//*[@categid]", Filters.element());
    }

    @Override
    protected Stream<MCRCategory> getCategories(MCRCategoryDAO dao, Document metadataDocument) {
        return CLASSIFICATION_ELEMENT_XPATH.evaluate(metadataDocument).stream()
            .map(classificationElement -> loadClassification(dao, classificationElement));
    }

    private MCRCategory loadClassification(MCRCategoryDAO dao, Element classificationElement) {
        return dao.getCategory(new MCRCategoryID(classificationElement.getAttributeValue("classid"),
            classificationElement.getAttributeValue("categid")), 0);
    }

    @Override
    protected MCRCategoryID toMappedValue(XMapping mapping) {
        return mapping.targetCategoryId();
    }

}
