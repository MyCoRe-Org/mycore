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

package org.mycore.wfc.actionmapping;

import org.apache.logging.log4j.LogManager;
import org.jdom2.Element;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.parsers.bool.MCRCondition;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRCategoryCondition implements MCRCondition<MCRWorkflowData> {

    private MCRCategoryID mcrCategoryID;

    private boolean not;

    private String fieldName;

    private static MCRCategLinkService LINK_SERVICE = MCRCategLinkServiceFactory.getInstance();

    public MCRCategoryCondition(String fieldName, MCRCategoryID mcrCategoryID, boolean not) {
        this.fieldName = fieldName;
        this.mcrCategoryID = mcrCategoryID;
        this.not = not;

    }

    /* (non-Javadoc)
     * @see org.mycore.parsers.bool.MCRCondition#evaluate(java.lang.Object)
     */
    @Override
    public boolean evaluate(MCRWorkflowData workflowData) {
        MCRCategLinkReference reference = workflowData.getCategoryReference();
        if (reference == null) {
            LogManager.getLogger(getClass())
                .error("Cannot evaluate '{}', if MCRWorkflowData does not contain an object reference",
                    toString());
            return false;
        }
        return LINK_SERVICE.isInCategory(reference, mcrCategoryID) ^ not;
    }

    @Override
    public String toString() {
        return fieldName + (not ? " != " : " = ") + mcrCategoryID.getID() + " ";
    }

    /* (non-Javadoc)
     * @see org.mycore.parsers.bool.MCRCondition#toXML()
     */
    @Override
    public Element toXML() {
        Element cond = new Element("condition");
        cond.setAttribute("field", fieldName);
        cond.setAttribute("operator", (not ? "!=" : "="));
        cond.setAttribute("value", mcrCategoryID.getID());
        return cond;
    }

}
