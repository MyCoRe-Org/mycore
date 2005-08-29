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

package org.mycore.parsers.bool;

import org.jdom.Element;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for parsing Boolean clauses
 * 
 * @author Matthias Kramm
 */

public class MCRBooleanClauseParser {
    private static Pattern bracket = Pattern.compile("\\([^)(]*\\)");

    private static Pattern and = Pattern.compile("\\band\\b");

    private static Pattern or = Pattern.compile("\\bor\\b");

    private static Pattern not = Pattern.compile("\\s*\\bnot\\b\\s*");

    private static Pattern marker = Pattern.compile("@<([0-9]*)>@");

    private static String extendClauses(String s, List l) {
        while (true) {
            Matcher m = marker.matcher(s);
            if (m.find()) {
                String c = m.group();
                String clause = (String) l.get(Integer.parseInt(m.group(1)));
                s = s.replaceAll(c, clause);
            } else {
                break;
            }
        }
        return s;
    }

    public MCRCondition parse( Element condition )
    {
        String name = condition.getName();
        if( name.equals( "not" ) )
        {
            Element child = (Element)( condition.getChildren().get( 0 ) );
            return new MCRNotCondition( parse( child ) );
        }
        else if(name.equals( "and" ) || name.equals("or"))
        {
            List children = condition.getChildren();
            MCRCondition cond;
            if(name.equals("and")) {
                MCRAndCondition acond = new MCRAndCondition();
                for( int i = 0; i < children.size(); i++ )
                {
                    Element child = (Element)( children.get( i ) );
                    acond.addChild( parse(child));
                }
                cond = acond;
            } else {
                MCROrCondition ocond = new MCROrCondition();
                for( int i = 0; i < children.size(); i++ )
                {
                    Element child = (Element)( children.get( i ) );
                    ocond.addChild( parse(child));
                }
                cond = ocond;
            }
            return cond;
        }
        else return parseSimpleCondition(condition);
    }

    public MCRCondition parse(String s)
            throws MCRParseException {
        s = s.replaceAll("\t", " ").replaceAll("\n", " ").replaceAll("\r", " ");
        return parse(s, null);
    }

    private MCRCondition parse(String s, List l)
            throws MCRParseException {
        if (l == null) {
            l = new ArrayList();
        }
        s = s.trim();

        /* replace all bracket expressions with $n */
        while (true) {
            /* remove outer brackets () */
            while (s.charAt(0) == '(' && s.charAt(s.length() - 1) == ')'
                    && s.substring(1, s.length() - 1).indexOf('(') < 0
                    && s.substring(1, s.length() - 1).indexOf(')') < 0) {
                s = s.substring(1, s.length() - 1).trim();
            }
            Matcher m = bracket.matcher(s);
            if (m.find()) {
                String clause = m.group();
                s = s.substring(0, m.start()) + "@<" + l.size() + ">@"
                        + s.substring(m.end());
                l.add(extendClauses(clause, l));
            } else {
                break;
            }
        }

        /* handle OR */
        Matcher m = or.matcher(s);
        if (m.find()) {
            if (m.start() <= 0)
                throw new MCRParseException(
                        "left side of OR missing while parsing \"" + s + "\"");
            if (m.end() >= s.length() - 1)
                throw new MCRParseException(
                        "right side of OR missing while parsing \"" + s + "\"");
            MCRCondition left = parse(extendClauses(s.substring(0, m
                    .start()), l), l);
            MCRCondition right = parse(extendClauses(s.substring(m
                    .end()), l), l);
            return new MCROrCondition(left, right);
        }
        /* handle AND */
        m = and.matcher(s);
        if (m.find()) {
            if (m.start() <= 0)
                throw new MCRParseException(
                        "left side of AND missing while parsing \"" + s + "\"");
            if (m.end() >= s.length() - 1)
                throw new MCRParseException(
                        "right side of AND missing while parsing \"" + s + "\"");
            MCRCondition left = parse(extendClauses(s.substring(0, m
                    .start()), l), l);
            MCRCondition right = parse(extendClauses(s.substring(m
                    .end()), l), l);
            return new MCRAndCondition(left, right);
        }
        /* handle NOT */
        s = s.trim();
        if (s.toLowerCase().startsWith("not ")) {
            MCRCondition inverse = parse(extendClauses(s
                    .substring(4), l), l);
            return new MCRNotCondition(inverse);
        }

        return parseSimpleCondition(s);
    }

    public MCRCondition parseSimpleCondition(String s) throws MCRParseException
    {
        /* handle specific rules */
        if (s.equalsIgnoreCase("false"))
            return new MCRFalseCondition();
        if (s.equalsIgnoreCase("true"))
            return new MCRTrueCondition();
        throw new MCRParseException("syntax error: " + s); //extendClauses(s, l));
    }
    
    public MCRCondition parseSimpleCondition(Element e) throws MCRParseException
    {
        String name = e.getName();
        if(name.equals("true"))
            return new MCRTrueCondition();
        if(name.equals("true"))
            return new MCRFalseCondition();
        throw new MCRParseException("syntax error: <" + name + ">");
    }
}
