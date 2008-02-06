/*
 * 
 * $Revision$ $Date$
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

package org.mycore.access.mcrimpl;

import org.jdom.Attribute;
import org.jdom.Element;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRConditionVisitor;

/**
 * Implementation of a (user xy) clause
 * 
 * @author Matthias Kramm
 */
class MCRUserClause implements MCRCondition {
    private String user;
    private boolean not;

    MCRUserClause(String user, boolean not) {
        this.user = user;
        this.not = not;
    }

    public boolean evaluate(Object o) {
        MCRAccessData data = (MCRAccessData) o;
        return this.user.equals(data.getUser().getID()) ^ this.not;
    }

    public String toString() {
        return "user" + (this.not? " != ": " = ") + user + " ";
    }

    public Element toXML() {
    	Element cond = new Element("condition");
    	cond.setAttribute("field", "user");
    	cond.setAttribute("operator", (this.not? "!=": "="));
    	cond.setAttribute("value", user);
        return cond;
    }

    public Element info() {
        Element el = new Element("info");
        el.setAttribute(new Attribute("type", "USER"));
        return el;
    }

    public void accept(MCRConditionVisitor visitor) { 
    	visitor.visitType(info());
    }
};
