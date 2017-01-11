/*
 * $Id$
 * $Revision: 5697 $ $Date: 09.03.2012 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
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
            LogManager.getLogger(getClass()).error(
                "Cannot evaluate '" + toString() + "', if MCRWorkflowData does not contain an object reference");
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
