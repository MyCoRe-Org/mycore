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
import java.util.regex.Pattern;

import org.jdom2.Element;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.parsers.bool.MCRCondition;

/**
 * Implementation of a (user xy) clause
 *
 * @author Matthias Kramm
 */
class MCRUserClause implements MCRCondition<Object> {
    private String user;

    private Pattern userRegEx;

    private boolean not;

    MCRUserClause(String user, boolean not) {
        if (user.contains("*")) {
            userRegEx = toRegex(user);
        }
        this.user = user;
        this.not = not;
    }

    private Pattern toRegex(String userExp) {
        StringBuilder regex = new StringBuilder("^");

        for (int i = 0; i < userExp.length(); i++) {
            char c = userExp.charAt(i);
            if (c == '*') {
                regex.append(".*");
            } else {
                regex.append(c);
            }
        }

        regex = regex.append('$');
        return Pattern.compile(regex.toString());
    }

    @Override
    public boolean evaluate(Object o) {
        MCRUserInformation userInformation = Optional.ofNullable(o)
            .filter(obj -> obj instanceof MCRAccessData)
            .map(MCRAccessData.class::cast)
            .map(MCRAccessData::getUserInformation)
            .orElseGet(MCRSessionMgr.getCurrentSession()::getUserInformation);
        if (userRegEx != null) {
            return userRegEx.matcher(userInformation.getUserID()).matches() ^ not;
        }
        return user.equals(userInformation.getUserID()) ^ not;
    }

    @Override
    public String toString() {
        return "user" + (not ? " != " : " = ") + user + " ";
    }

    @Override
    public Element toXML() {
        Element cond = new Element("condition");
        cond.setAttribute("field", "user");
        cond.setAttribute("operator", (not ? "!=" : "="));
        cond.setAttribute("value", user);
        return cond;
    }
}
