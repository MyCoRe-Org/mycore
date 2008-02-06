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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jdom.Attribute;
import org.jdom.Element;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRConditionVisitor;

/**
 * Implementation of a (date &gt; xy) clause
 * 
 * @author Matthias Kramm
 */
class MCRDateAfterClause implements MCRCondition {
    private Date date;
    private static DateFormat dateformat = new SimpleDateFormat("dd.MM.yyyy");

    MCRDateAfterClause(Date date) {
        this.date = date;
    }

    public boolean evaluate(Object o) {
        MCRAccessData data = (MCRAccessData) o;

        return data.getDate().after(this.date);
    }

    public String toString() {
        return "date > " + dateformat.format(date) + " ";
    }

    public Element toXML() {
    	Element cond = new Element("condition");
    	cond.setAttribute("field", "date");
    	cond.setAttribute("operator", ">");
    	cond.setAttribute("value", dateformat.format(date));
        return cond;
    }

    public Element info() {
        Element el = new Element("info");
        el.setAttribute(new Attribute("type", "DATE"));
        return el;
    }

    public void accept(MCRConditionVisitor visitor) {
    	visitor.visitType(info());
    }
};
