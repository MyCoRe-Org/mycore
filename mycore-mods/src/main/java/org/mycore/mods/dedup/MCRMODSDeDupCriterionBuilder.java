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

package org.mycore.mods.dedup;

import java.util.List;
import java.util.Set;

import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.dedup.MCRDeDupCriterion;
import org.mycore.dedup.MCRDeDupCriterionBuilder;
import org.mycore.mods.MCRMODSWrapper;

/**
 * Base class for {@link MCRDeDupCriterionBuilder}s that derive deduplication criteria from the MODS
 * metadata of an object. Subclasses implement {@link #buildFromMODS(Element)} to extract one specific
 * kind of criterion from the {@code mods:mods} element.
 */
public abstract class MCRMODSDeDupCriterionBuilder implements MCRDeDupCriterionBuilder {

    @Override
    public Set<MCRDeDupCriterion> build(MCRObject object) {
        if (!MCRMODSWrapper.isSupported(object)) {
            return Set.of();
        }
        Element mods = new MCRMODSWrapper(object).getMODS();
        if (mods == null) {
            return Set.of();
        }
        return buildFromMODS(mods);
    }

    /**
     * Builds the deduplication criteria contributed by this builder from the given MODS element.
     *
     * @param mods the {@code mods:mods} element of the object
     * @return the criteria extracted from the MODS metadata, or an empty set if none apply
     */
    public abstract Set<MCRDeDupCriterion> buildFromMODS(Element mods);

    /**
     * Evaluates the given XPath expression against a context element and returns the matching elements.
     *
     * @param context the context element the XPath is evaluated against
     * @param xPath   the XPath expression selecting elements
     * @return the list of matching elements
     */
    protected List<Element> getNodes(Element context, String xPath) {
        XPathExpression<Element> expression = XPathFactory.instance().compile(xPath, Filters.element(), null,
            MCRConstants.getStandardNamespaces());
        return expression.evaluate(context);
    }
}
