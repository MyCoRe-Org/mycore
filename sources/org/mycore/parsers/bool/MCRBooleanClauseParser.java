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

package org.mycore.parsers.bool;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;

/**
 * Class for parsing Boolean clauses
 * 
 * @author Matthias Kramm
 */
public class MCRBooleanClauseParser {
    private static Pattern bracket = Pattern.compile("\\([^)(]*\\)");
    
    private static Pattern and = Pattern.compile("\\b[aA][nN][dD]\\b");

    private static Pattern or = Pattern.compile("\\b[oO][rR]\\b");

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

    public MCRCondition parse(Element condition) {
        if (condition == null) {
            return defaultRule();
        }

        if (condition.getName().toLowerCase().equals("boolean")) {
            String name = condition.getAttributeValue("operator").toLowerCase();

            if (name.equals("not")) {
                Element child = (Element) (condition.getChildren().get(0));
                return new MCRNotCondition(parse(child));
            } else if (name.equals("and") || name.equals("or")) {
                List children = condition.getChildren();
                MCRCondition cond;

                if (name.equals("and")) {
                    MCRAndCondition acond = new MCRAndCondition();

                    for (int i = 0; i < children.size(); i++) {
                        Element child = (Element) (children.get(i));
                        acond.addChild(parse(child));
                    }

                    cond = acond;
                } else {
                    MCROrCondition ocond = new MCROrCondition();

                    for (int i = 0; i < children.size(); i++) {
                        Element child = (Element) (children.get(i));
                        ocond.addChild(parse(child));
                    }

                    cond = ocond;
                }

                return cond;
            } else {
                return parseSimpleCondition(condition);
            }        	
        }
        return parseSimpleCondition(condition);
    }

    public MCRCondition parse(String s) throws MCRParseException {
        s = s.replaceAll("\t", " ").replaceAll("\n", " ").replaceAll("\r", " ");

        if (s.trim().length() == 0) {
            return defaultRule();
        }

        return parse(s, null);
    }

    private MCRCondition parse(String s, List l) throws MCRParseException {
        if (l == null) {
            l = new ArrayList();
        }

        s = s.trim();

        /* replace all bracket expressions with $n */
        while (true) {
            /* remove outer brackets () */
            while ((s.charAt(0) == '(') && (s.charAt(s.length() - 1) == ')') && (s.substring(1, s.length() - 1).indexOf('(') < 0) && (s.substring(1, s.length() - 1).indexOf(')') < 0)) {
                s = s.substring(1, s.length() - 1).trim();
            }

            Matcher m = bracket.matcher(s);

            if (m.find()) {
                String clause = m.group();
                s = s.substring(0, m.start()) + "@<" + l.size() + ">@" + s.substring(m.end());
                l.add(extendClauses(clause, l));
            } else {
                break;
            }
        }

        /* handle OR */
        Matcher m = or.matcher(s);
        int last = 0;
        MCROrCondition orclause = new MCROrCondition();
        while(m.find()) {
            int l1 = m.start();
            if (last >= l1) {
                throw new MCRParseException("subclause of OR missing while parsing \"" + s + "\"");
            }
            MCRCondition c = parse(extendClauses(s.substring(last, l1), l) , l);
            last = m.end();
            orclause.addChild(c);
        }
        if(last!=0) {
            MCRCondition c = parse(extendClauses(s.substring(last), l) , l);
            orclause.addChild(c);
            return orclause;
        }

        /* handle AND */
        m = and.matcher(s);
        last = 0;
        MCRAndCondition andclause = new MCRAndCondition();
        while(m.find()) {
            int l1 = m.start();
            if (last >= l1) {
                throw new MCRParseException("subclause of AND missing while parsing \"" + s + "\"");
            }
            MCRCondition c = parse(extendClauses(s.substring(last, l1), l) , l);
            last = m.end();
            andclause.addChild(c);
        }
        if(last!=0) {
            MCRCondition c = parse(extendClauses(s.substring(last), l) , l);
            andclause.addChild(c);
            return andclause;
        }

        /* handle NOT */
        s = s.trim();

        if (s.toLowerCase().startsWith("not ")) {
            MCRCondition inverse = parse(extendClauses(s.substring(4), l), l);

            return new MCRNotCondition(inverse);
        }

        s = extendClauses(s,l);
        if(s.indexOf('(') >= 0)
            return parse(s,l);
        else
            return parseSimpleCondition(s);
    }

    protected MCRCondition parseSimpleCondition(String s) throws MCRParseException {
        /* handle specific rules */
        s = s.toLowerCase();
        if (s.equalsIgnoreCase("true")) {
            return new MCRTrueCondition();
        }

        if (s.equalsIgnoreCase("false")) {
            return new MCRFalseCondition();
        }

        throw new MCRParseException("syntax error: " + s); // extendClauses(s,
                                                            // l));
    }

    protected MCRCondition parseSimpleCondition(Element e) throws MCRParseException {
    	// <boolean operator="true|false" />
        String name = e.getAttributeValue("operator").toLowerCase();

        if (name.equals("true")) {
            return new MCRTrueCondition();
        }

        if (name.equals("false")) {
            return new MCRFalseCondition();
        }

        throw new MCRParseException("syntax error: <" + name + ">");
    }

    protected MCRCondition defaultRule() {
        return new MCRTrueCondition();
    }

    public static void main(String[] args)
    {
        MCRBooleanClauseParser p = new MCRBooleanClauseParser();
        System.out.println(p.parse("true or false or true").toString());
        System.out.println(p.parse("(true) or (false) or (true)").toString());
        System.out.println(p.parse("true and false and true").toString());
        System.out.println(p.parse("true or false and true").toString());
        System.out.println(p.parse("true or true or (true or true)").toString());
        System.out.println(p.parse("((true))").toString());
        System.out.println(p.parse("(true ) or  ( ((false) or (true)))").toString());
    }
}
