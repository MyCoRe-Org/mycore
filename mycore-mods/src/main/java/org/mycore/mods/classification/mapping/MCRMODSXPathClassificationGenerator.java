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
package org.mycore.mods.classification.mapping;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.jdom2.Parent;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.mapping.MCRXPathClassificationGeneratorBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;

/**
 * A {@link MCRMODSXMappingClassificationGenerator} is a {@link MCRMODSGeneratorClassificationMapper.Generator}
 * that looks for mapping information for a given list of classifications, by checking if XPaths configured
 * for categories of such classifications match the MODS document.
 * <p>
 * For each classification, the set of classification categories containing a <code>x-mapping-xpath</code> label is
 * obtained. For all such classification categories, the corresponding XPath is evaluated and if the MODS document
 * matches, the classification category is provided. If no classification is provided for a given classification,
 * the same procedure is performed for the <code>x-mapping-xpathfb</code> labes as a fallback.
 * </p>
 * Example form <code>foo_bar</code>:
 * <pre><code>
 * &lt;category ID=&quot;article&quot;&gt;
 * &nbsp;&lt;label xml:lang=&quot;en&quot; text=&quot;Article&quot; /&gt;
 * &nbsp;&lt;label xml:lang=&quot;x-mapping-xpath&quot; text=&quot;mods:genre[text()='article']&quot; /&gt;
 * &nbsp;&lt;label xml:lang=&quot;x-mapping-xpathfb&quot; text=&quot;mods:genre[text()='other']&quot; /&gt;
 * &lt;/category&gt;
 * </code></pre>
 * If <code>foo_bar</code> is in the list of classifications, the classification category
 * <code>foo_bar:article</code> will be provided, if the XPath <code>mods:genre[text()='article']</code> is matching
 * (or if no other classification category has a matching XPath and <code>mods:genre[text()='other']</code> is
 * matching). The corresponding generator will be named <code>xpathmapping2foo_bar</code>.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRXPathClassificationGeneratorBase#CLASSIFICATION_IDS_KEY} can be used to
 * specify the comma separated list of classifications IDs.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.mods.classification.mapping.MCRMODSXMappingClassificationGenerator
 * [...].ClassificationIDs=foo_bar,foo_baz
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRMODSXPathClassificationGenerator.Factory.class)
public final class MCRMODSXPathClassificationGenerator extends MCRXPathClassificationGeneratorBase {

    public MCRMODSXPathClassificationGenerator(String... classificationsIds) {
        this(Arrays.asList(Objects.requireNonNull(classificationsIds, "Classification IDs must not be null")));
    }

    public MCRMODSXPathClassificationGenerator(List<String> classificationsIds) {
        super(classificationsIds);
    }

    @Override
    protected Parent toJdomParent(MCRCategoryDAO dao, MCRObject object) {
        return new MCRMODSWrapper(object).getMODS();
    }

    public static class Factory implements Supplier<MCRMODSXPathClassificationGenerator> {

        @MCRProperty(name = CLASSIFICATION_IDS_KEY, required = false)
        public String classificationIds;

        @Override
        public MCRMODSXPathClassificationGenerator get() {
            return new MCRMODSXPathClassificationGenerator(getClassificationsIds());
        }

        private List<String> getClassificationsIds() {
            return MCRConfiguration2.splitValue(classificationIds == null ? "" : classificationIds).toList();
        }

    }

}
