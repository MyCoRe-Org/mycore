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

package org.mycore.access.mcrimpl;

import java.util.Optional;

import org.jdom2.Element;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.parsers.bool.MCRCondition;

/**
 * Implementation of a (group xy) clause
 *
 * @author Matthias Kramm
 * @author Mathias Fricke
 */
class MCRGroupClause implements MCRCondition<Object> {

    private String groupname;

    private boolean not;

    MCRGroupClause(String group, boolean not) {
        groupname = group;
        this.not = not;
    }

    @Override
    public boolean evaluate(Object o) {
        MCRUserInformation userInformation = Optional.ofNullable(o)
            .filter(obj -> obj instanceof MCRAccessData)
            .map(MCRAccessData.class::cast)
            .map(MCRAccessData::getUserInformation)
            .orElseGet(MCRSessionMgr.getCurrentSession()::getUserInformation);
        return userInformation.isUserInRole(groupname) ^ not;
    }

    @Override
    public String toString() {
        return "group" + (not ? " != " : " = ") + groupname + " ";
    }

    @Override
    public Element toXML() {
        Element cond = new Element("condition");
        cond.setAttribute("field", "group");
        cond.setAttribute("operator", (not ? "!=" : "="));
        cond.setAttribute("value", groupname);
        return cond;
    }
}
