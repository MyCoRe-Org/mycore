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
package org.mycore.datamodel.classifications2.mapping;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.mapping.MCRGeneratorClassificationMapperBase.Generator;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * A {@link MCRDefaultXMappingClassificationGenerator} is a {@link Generator} that looks for mapping information
 * in all classification values already present in the default metadata document.
 * <p>
 * For each classification value, if the corresponding classification category contains a <code>x-mapping</code>
 * label, the content of that label is used as a space separated list of classification category IDs.
 * </p>
 * Example form <code>foo_bar</code>:
 * <pre><code>
 * &lt;category ID=&quot;article&quot;&gt;
 *  &lt;label xml:lang=&quot;en&quot; text=&quot;ScolarlyArticle&quot; /&gt;
 *  &lt;label xml:lang=&quot;x-mapping&quot; text=&quot;foo_baz:Article&quot; /&gt;
 * &lt;/category&gt;
 * </code></pre>
 * If a default metadata document contains the classification category <code>foo_bar:article</code>, the
 * classification category <code>foo_baz:ScolarlyArticle</code> will be provided.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRXMappingClassificationGeneratorBase#ON_MISSING_MAPPED_CATEGORY_KEY} can be used to
 * specify the behaviour, when a mapped classification value is missing.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.datamodel.classifications2.mapping.MCRDefaultXMappingClassificationGenerator
 * [...].OnMissingMappedCategory=WARN_AND_IGNORE
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRDefaultXMappingClassificationGenerator.Factory.class)
public final class MCRDefaultXMappingClassificationGenerator extends MCRXMappingClassificationGeneratorBase {

    private static final XPathExpression<Element> CLASSIFICATION_ELEMENT_XPATH;

    static {
        CLASSIFICATION_ELEMENT_XPATH = XPathFactory.instance().compile("//*[@categid]", Filters.element());
    }

    public MCRDefaultXMappingClassificationGenerator(OnMissingMappedCategory onMissingMappedCategory) {
        super(onMissingMappedCategory);
    }

    @Override
    public boolean isSupported(MCRObject object) {
        return true;
    }

    @Override
    protected Stream<MCRCategory> getCategories(MCRCategoryDAO dao, MCRObject object) {
        Document metadataDocument = getContext(object);
        return CLASSIFICATION_ELEMENT_XPATH.evaluate(metadataDocument).stream()
            .map(classificationElement -> loadClassification(dao, classificationElement));
    }

    private static Document getContext(MCRObject object) {
        return new Document(object.getMetadata().createXML());
    }

    private MCRCategory loadClassification(MCRCategoryDAO dao, Element classificationElement) {
        return dao.getCategory(new MCRCategoryID(classificationElement.getAttributeValue("classid"),
            classificationElement.getAttributeValue("categid")), 0);
    }

    public static class Factory implements Supplier<MCRDefaultXMappingClassificationGenerator> {

        @MCRProperty(name = ON_MISSING_MAPPED_CATEGORY_KEY)
        public String onMissingMappedCategory;

        @Override
        public MCRDefaultXMappingClassificationGenerator get() {
            return new MCRDefaultXMappingClassificationGenerator(getOnMissingMappedCategory());
        }

        private OnMissingMappedCategory getOnMissingMappedCategory() {
            return OnMissingMappedCategory.valueOf(this.onMissingMappedCategory);
        }

    }

}
