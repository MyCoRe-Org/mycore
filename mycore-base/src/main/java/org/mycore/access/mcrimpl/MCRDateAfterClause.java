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

package org.mycore.access.mcrimpl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.jdom2.Element;
import org.mycore.parsers.bool.MCRCondition;

/**
 * Implementation of a (date &gt; xy) clause
 * 
 * @author Matthias Kramm
 */
class MCRDateAfterClause implements MCRCondition<MCRAccessData> {
    private Date date;

    private DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

    MCRDateAfterClause(Date date) {
        this.date = date;
    }

    public boolean evaluate(MCRAccessData data) {
        return data.getDate().after(date);
    }

    @Override
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

}
