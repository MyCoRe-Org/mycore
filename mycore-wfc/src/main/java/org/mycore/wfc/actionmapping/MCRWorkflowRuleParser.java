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

import org.jdom2.Element;
import org.mycore.access.mcrimpl.MCRRuleParser;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRParseException;
import org.mycore.wfc.MCRConstants;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRWorkflowRuleParser extends MCRRuleParser {

    private static final String EQUALS = "=";

    private static final String EQUALS_NOT = "!=";

    private static final String STATUS = "status";

    private MCRCategoryID statusClassId;

    protected MCRWorkflowRuleParser() {
        super();
        this.statusClassId = MCRConstants.STATUS_CLASS_ID;
    }

    /* (non-Javadoc)
     * @see org.mycore.access.mcrimpl.MCRRuleParser#parseElement(org.jdom2.Element)
     */
    @Override
    protected MCRCondition<?> parseElement(Element e) {
        String field = e.getAttributeValue("field");
        String operator = e.getAttributeValue("operator");
        String value = e.getAttributeValue("value");
        boolean not = EQUALS_NOT.equals(operator);
        if (STATUS.equals(field)) {
            return new MCRCategoryCondition(STATUS, new MCRCategoryID(statusClassId.getRootID(), value), not);
        }
        return super.parseElement(e);
    }

    /* (non-Javadoc)
     * @see org.mycore.access.mcrimpl.MCRRuleParser#parseString(java.lang.String)
     */
    @Override
    protected MCRCondition<?> parseString(String s) {
        if (s.startsWith(STATUS)) {
            s = s.substring(STATUS.length()).trim();
            boolean not;
            String value;
            if (s.startsWith(EQUALS_NOT)) {
                not = true;
                value = s.substring(EQUALS_NOT.length()).trim();
            } else if (s.startsWith(EQUALS)) {
                not = false;
                value = s.substring(EQUALS.length()).trim();
            } else {
                throw new MCRParseException("syntax error: " + s);
            }
            return new MCRCategoryCondition(STATUS, new MCRCategoryID(statusClassId.getRootID(), value), not);
        }
        return super.parseString(s);
    }

}
