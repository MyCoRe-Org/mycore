/*
 * $RCSfile$
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

package org.mycore.services.fieldquery;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;
import org.mycore.parsers.bool.MCRBooleanClauseParser;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRParseException;

/**
 * Parses query conditions for use in MCRSearcher.
 * 
 * @see MCRSearcher
 * 
 * @author Frank Lützenkirchen
 */
public class MCRQueryParser extends MCRBooleanClauseParser {

    /**
     * Parses XML element containing a simple query condition
     * 
     * @param e
     *            the 'condition' element
     * @return the parsed MCRQueryCondition object
     */
    protected MCRCondition parseSimpleCondition(Element e) throws MCRParseException {
        String name = e.getName();

        if (!name.equals("condition"))
            throw new MCRParseException("Not a valid <" + name + ">");

        String field = e.getAttributeValue("field");
        String opera = e.getAttributeValue("operator");
        String value = e.getAttributeValue("value");

        MCRFieldDef def = MCRFieldDef.getDef(field);
        if (def == null)
            throw new MCRParseException("Field not defined: <" + field + ">");

        return new MCRQueryCondition(def, opera, value);
    }

    /** Pattern for MCRQueryConditions expressed as String */
    private static Pattern pattern = Pattern.compile("([^ \t\r\n]+)\\s+([^ \t\r\n]+)\\s+([^ \"\t\r\n]+|\"[^\"]*\")");

    /**
     * Parses a String containing a simple query condition, for example: (title
     * contains "Java") and (creatorID = "122132131")
     * 
     * @param s
     *            the condition as a String
     * @return the parsed MCRQueryCondition object
     */
    protected MCRCondition parseSimpleCondition(String s) throws MCRParseException {
        Matcher m = pattern.matcher(s);

        if (!m.find())
            throw new MCRParseException("Not a valid condition: " + s);

        String field = m.group(1);
        String operator = m.group(2);
        String value = m.group(3);

        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }

        MCRFieldDef def = MCRFieldDef.getDef(field);
        if (def == null)
            throw new MCRParseException("Field not defined: <" + field + ">");

        return new MCRQueryCondition(def, operator, value);
    }
    
    /** Used for input validation in editor search form */
    public static boolean validateQueryExpression( String query )
    {
      try
      {  
        MCRCondition cond = new MCRQueryParser().parse( query );
        return( cond != null );
      }
      catch( Exception ex )
      { return false; }
    }
}
