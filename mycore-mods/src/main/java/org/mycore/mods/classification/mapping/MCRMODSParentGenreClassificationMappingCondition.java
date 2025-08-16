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
package org.mycore.mods.classification.mapping;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.mapping.MCRConditionalXMappingEvaluator.Condition;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;

/**
 * A {@link MCRMODSParentGenreClassificationMappingCondition} is a {@link Condition} that
 * determines the genres of MODS related items with type <code>seris</code> or <code>host</code>.
 * <p>
 * No configuration options are available.
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.mods.classification.mapping.MCRMODSParentGenreClassificationMappingCondition
 * </code></pre>
 */
public final class MCRMODSParentGenreClassificationMappingCondition implements Condition {

    private final String genreAuthorityUrl;

    private final String parentGenreXpath;

    public MCRMODSParentGenreClassificationMappingCondition() {

        MCRCategoryDAO dao = MCRCategoryDAOFactory.obtainInstance();
        MCRCategory genresCategory = dao.getRootCategory(new MCRCategoryID("mir_genres"), 0);

        if (genresCategory == null) {
            throw new MCRException("Missing classification: mir_genres");
        }

        Optional<MCRLabel> uriLabel = genresCategory.getLabel("x-uri");
        if (uriLabel.isEmpty()) {
            throw new MCRException("Missing classification label 'x-uri': mir_genres");
        }

        genreAuthorityUrl = uriLabel.get().getText();
        parentGenreXpath = "mods:relatedItem[@type='series' or @type='host']"
            + "/mods:genre[@type='intern' and @authorityURI='" + genreAuthorityUrl + "']";

    }

    @Override
    public Set<String> evaluate(MCRObject object) {

        Set<String> parentGenres = new LinkedHashSet<>();

        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        List<Element> parentGenreElements = wrapper.getElements(parentGenreXpath);
        for (Element parentGenreElement : parentGenreElements) {
            String parentGenreUrl = parentGenreElement.getAttributeValue("valueURI");
            if (parentGenreUrl != null) {
                int parentGenreAuthorityPrefixLength = genreAuthorityUrl.length() + 1;
                if (parentGenreUrl.length() > parentGenreAuthorityPrefixLength) {
                    String parentGenre = parentGenreUrl.substring(parentGenreAuthorityPrefixLength);
                    parentGenres.add(parentGenre);
                }
            }
        }

        return parentGenres;

    }

}
