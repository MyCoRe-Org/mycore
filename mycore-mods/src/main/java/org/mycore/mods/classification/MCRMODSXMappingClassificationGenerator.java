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
package org.mycore.mods.classification;

import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRXMappingClassificationGeneratorBase;
import org.mycore.mods.MCRMODSWrapper;

/**
 * A {@link MCRMODSXMappingClassificationGenerator} is a {@link MCRMODSClassificationMapper.Generator}
 * that looks for mapping information in all classification values already present in the MODS document.
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
 * If a MODS document contains the classification category <code>foo_bar:article</code>, the
 * classification category <code>foo_baz:ScolarlyArticle</code> will be provided. The corresponding generator
 * will be named <code>foo_bar2foo_baz</code>.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRXMappingClassificationGeneratorBase#ON_MISSING_MAPPED_CATEGORY_KEY} can be used to
 * specify the behaviour, when a mapped classification value is missing.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.mods.classification.MCRMODSXMappingClassificationGenerator
 * [...].OnMissingMappedCategory=WARN_AND_IGNORE
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRMODSXMappingClassificationGenerator.Factory.class)
public final class MCRMODSXMappingClassificationGenerator extends MCRXMappingClassificationGeneratorBase<MCRMODSWrapper,
    MCRMODSClassificationMapper.Mapping> implements MCRMODSClassificationMapper.Generator {

    public MCRMODSXMappingClassificationGenerator(OnMissingMappedCategory onMissingMappedCategory) {
        super(onMissingMappedCategory);
    }

    @Override
    protected Stream<MCRCategory> getCategories(MCRCategoryDAO dao, MCRMODSWrapper modsWrapper) {
        return modsWrapper.getMcrCategoryIDs().stream().map(categoryId -> dao.getCategory(categoryId, 0));
    }

    @Override
    protected MCRMODSClassificationMapper.Mapping toMappedValue(XMapping mapping) {
        String generator = getGenerator(mapping.sourceCategoryId(), mapping.targetCategoryId());
        return new MCRMODSClassificationMapper.Mapping(generator, mapping.targetCategoryId());
    }

    private String getGenerator(MCRCategoryID sourceCategoryId, MCRCategoryID targetCategoryId) {
        return String.format(Locale.ROOT, "%s2%s", sourceCategoryId.getRootID(), targetCategoryId.getRootID());
    }

    public static class Factory implements Supplier<MCRMODSXMappingClassificationGenerator> {

        @MCRProperty(name = ON_MISSING_MAPPED_CATEGORY_KEY)
        public String onMissingMappedCategory;

        @Override
        public MCRMODSXMappingClassificationGenerator get() {

            OnMissingMappedCategory onMissingMappedCategory = OnMissingMappedCategory
                .valueOf(this.onMissingMappedCategory);

            return new MCRMODSXMappingClassificationGenerator(onMissingMappedCategory);

        }

    }

}
