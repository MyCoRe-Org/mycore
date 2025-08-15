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

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.mapping.MCRGeneratorClassificationMapperBase.Generator;
import org.mycore.datamodel.classifications2.mapping.MCRSimpleXMappingEvaluator;
import org.mycore.datamodel.classifications2.mapping.MCRXMappingClassificationGeneratorBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;

/**
 * A {@link MCRMODSXMappingClassificationGenerator} is a {@link Generator} that looks for
 * mapping information in all categories already present in the MODS document.
 * <p>
 * For each category ID in MODS document, if the corresponding category
 * has a <code>x-mapping</code>-label, the content of that label is used
 * to obtain additional category IDs using an {@link Evaluator}.
 * </p>
 * Example form <code>foo_bar</code>:
 * <pre><code>
 * &lt;category ID=&quot;article&quot;&gt;
 *  &lt;label xml:lang=&quot;en&quot; text=&quot;ScolarlyArticle&quot; /&gt;
 *  &lt;label xml:lang=&quot;x-mapping&quot; text=&quot;foo_baz:Article&quot; /&gt;
 * &lt;/category&gt;
 * </code></pre>
 * If a MODS document contains the classification category <code>foo_bar:article</code>, the
 * classification category <code>foo_baz:ScolarlyArticle</code> will be provided.
 * The corresponding generator will be named <code>foo_bar2foo_baz</code>.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRXMappingClassificationGeneratorBase#EVALUATOR_KEY} can be used to
 * specify the evaluator used to obtain category IDs from.
 * <li> For the evaluator, the property suffix {@link MCRSentinel#DEFAULT_KEY} can be used to
 *  exclude the evaluator from the configuration and use a default {@link MCRSimpleXMappingEvaluator} instead.
 * <li> The property suffix {@link MCRXMappingClassificationGeneratorBase#ON_MISSING_MAPPED_CATEGORY_KEY} can be used to
 * specify the behaviour, when a mapped classification value is missing.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.mods.classification.mapping.MCRMODSXMappingClassificationGenerator
 * [...].Evaluator.Class=foo.bar.FooEvaluator
 * [...].Evaluator.Default=false
 * [...].Evaluator.Key1=Value1
 * [...].Evaluator.Key2=Value2
 * [...].OnMissingMappedCategory=WARN_AND_IGNORE
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRMODSXMappingClassificationGenerator.Factory.class)
public final class MCRMODSXMappingClassificationGenerator extends MCRXMappingClassificationGeneratorBase {

    public MCRMODSXMappingClassificationGenerator(OnMissingMappedCategory onMissingMappedCategory) {
        super(new MCRSimpleXMappingEvaluator(), onMissingMappedCategory);
    }

    public MCRMODSXMappingClassificationGenerator(Evaluator evaluator,
        OnMissingMappedCategory onMissingMappedCategory) {
        super(evaluator, onMissingMappedCategory);
    }

    @Override
    public boolean isSupported(MCRObject object) {
        return MCRMODSWrapper.isSupported(object);
    }

    @Override
    protected Stream<MCRCategory> getCategories(MCRCategoryDAO dao, MCRObject object) {
        return new MCRMODSWrapper(object).getMcrCategoryIDs().stream()
            .map(categoryId -> dao.getCategory(categoryId, 0));
    }

    public static class Factory implements Supplier<MCRMODSXMappingClassificationGenerator> {

        @MCRInstance(name = EVALUATOR_KEY, valueClass = Evaluator.class, required = false,
            sentinel = @MCRSentinel(name = MCRSentinel.DEFAULT_KEY, rejectionValue = true, defaultValue = false))
        public Evaluator evaluator;

        @MCRProperty(name = ON_MISSING_MAPPED_CATEGORY_KEY)
        public String onMissingMappedCategory;

        @Override
        public MCRMODSXMappingClassificationGenerator get() {
            return new MCRMODSXMappingClassificationGenerator(getEvaluator(), getOnMissingMappedCategory());
        }

        private Evaluator getEvaluator() {
            return Objects.requireNonNullElseGet(evaluator, MCRSimpleXMappingEvaluator::new);
        }

        private OnMissingMappedCategory getOnMissingMappedCategory() {
            return OnMissingMappedCategory.valueOf(this.onMissingMappedCategory);
        }

    }

}
