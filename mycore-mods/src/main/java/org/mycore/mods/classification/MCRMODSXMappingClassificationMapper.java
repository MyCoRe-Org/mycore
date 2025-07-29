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
import java.util.stream.Stream;

import org.mycore.common.events.MCRXMappingClassificationMapperBase;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.mods.classification.MCRMODSClassificationMappingEventHandler.Mapper;
import org.mycore.mods.classification.MCRMODSClassificationMappingEventHandler.Mapping;

/**
 * A {@link MCRMODSXMappingClassificationMapper} is a {@link Mapper} that looks for mapping
 * information in all classification values already present in the MODS document.
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
 * No configuration options are available.
 * <p>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.mods.classification.MCRMODSXMappingClassificationMapper
 * </code></pre>
 */
public class MCRMODSXMappingClassificationMapper
    extends MCRXMappingClassificationMapperBase<MCRMODSWrapper, Mapping>
    implements Mapper {

    @Override
    protected Stream<MCRCategory> getCategories(MCRCategoryDAO dao, MCRMODSWrapper modsWrapper) {
        return modsWrapper.getMcrCategoryIDs().stream().map(categoryId -> dao.getCategory(categoryId, 0));
    }

    @Override
    protected Mapping toMappedValue(XMapping mapping) {
        String generator = getGenerator(mapping.sourceCategoryId(), mapping.targetCategoryId());
        return new Mapping(generator, mapping.targetCategoryId());
    }

    private String getGenerator(MCRCategoryID sourceCategoryId, MCRCategoryID targetCategoryId) {
        return String.format(Locale.ROOT, "%s2%s", sourceCategoryId.getRootID(), targetCategoryId.getRootID());
    }

}
