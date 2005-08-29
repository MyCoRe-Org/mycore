/**
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 **/

package org.mycore.access;

import org.mycore.parsers.bool.*;
import org.jdom.Element;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

class MCRRuleParser extends MCRBooleanClauseParser {

    MCRRuleParser()
    {
    }

    private static DateFormat dateformat = new SimpleDateFormat("dd.MM.yyyy");
    private static Date parseDate(String s, boolean dayafter)
            throws MCRParseException {
        try {
            long time = dateformat.parse(s).getTime();
            if (dayafter)
                time += 1000 * 60 * 60 * 24;
            return new Date(time);
        } catch (java.text.ParseException e) {
            throw new MCRParseException("unable to parse date " + s);
        }
    }

    public MCRCondition parseSimpleCondition(Element e) throws MCRParseException
    {
        /* XML parsing not implemented yet for access rules */
        return null;
    }

    public MCRCondition parseSimpleCondition(String s)
    {
        if (s.startsWith("group "))
            return new MCRGroupClause(s.substring(6).trim());
        if (s.startsWith("user "))
            return new MCRUserClause(s.substring(5).trim());
        if (s.startsWith("ip ")) {
            return new MCRIPClause(s.substring(3).trim());
        }
        if (s.startsWith("date ")) {
            s = s.substring(5).trim();
            if (s.startsWith(">="))
                return new MCRDateAfterClause(parseDate(s.substring(2).trim(),
                        false));
            else if (s.startsWith("<="))
                return new MCRDateBeforeClause(parseDate(s.substring(2).trim(),
                        true));
            else if (s.startsWith(">"))
                return new MCRDateAfterClause(parseDate(s.substring(1).trim(),
                        true));
            else if (s.startsWith("<"))
                return new MCRDateBeforeClause(parseDate(s.substring(1).trim(),
                        false));
        }
        throw new MCRParseException("syntax error: " + s);
    }

};
