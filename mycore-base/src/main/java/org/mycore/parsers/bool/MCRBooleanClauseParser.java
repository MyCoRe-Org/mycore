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

package org.mycore.parsers.bool;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Element;

/**
 * Class for parsing Boolean clauses
 * 
 * @author Matthias Kramm
 * @author Christoph Neidahl (OPNA2608)
 */
public class MCRBooleanClauseParser<T> {
    private static Pattern bracket = Pattern.compile("\\([^)(]*\\)");

    private static Pattern apostrophe = Pattern.compile("\"[^\"]*?\"");

    private static Pattern and = Pattern.compile("[)\\s]+[aA][nN][dD][\\s(]+");

    private static Pattern or = Pattern.compile("[)\\s]+[oO][rR][\\s(]+");

    private static Pattern bracket_marker = Pattern.compile("@<([0-9]*)>@");
    
    private static String opening_bracket = "%%%%%%%%%%";

    private static String closing_bracket = "##########";

    private static String extendClauses(String s, List<String> l) {
        while (true) {
            Matcher m = bracket_marker.matcher(s);

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

    /**
     * Parse a complex or simple condition in XML format and put it in an condition object.
     * for samples see {@link org.mycore.services.fieldquery.MCRQueryParserTest#testQueryAsXML()}
     * @param condition a MyCoRe condition object in XML format
     * @return a MyCoRe condition object in the MCRCondition format
     */
    public MCRCondition<T> parse(Element condition) {
        if (condition == null) {
            return defaultRule();
        }

        if (condition.getName().equalsIgnoreCase("boolean")) {
            String operator = condition.getAttributeValue("operator");
            if (operator == null) {
                throw new MCRParseException("Syntax error: attribute operator not found");
            }

            if (operator.equalsIgnoreCase("not")) {
                Element child = condition.getChildren().get(0);
                return new MCRNotCondition<>(parse(child));
            } else if (operator.equalsIgnoreCase("and") || operator.equalsIgnoreCase("or")) {
                List <Element> children = condition.getChildren();
                MCRCondition<T> cond;

                if (operator.equalsIgnoreCase("and")) {
                    MCRAndCondition<T> acond = new MCRAndCondition<>();

                    for (Object aChildren : children) {
                        Element child = (Element) aChildren;
                        acond.addChild(parse(child));
                    }

                    cond = acond;
                } else {
                    MCROrCondition<T> ocond = new MCROrCondition<>();

                    for (Object aChildren : children) {
                        Element child = (Element) aChildren;
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

    /**
     * Parse a complex or simple condition in String format and put it in an condition object.
     * for samples see {@link org.mycore.services.fieldquery.MCRQueryParserTest#testQueryAsString()}
     * @param s a MyCoRe condition object in String format
     * @return a MyCoRe condition object in the MCRCondition format
     */
    public MCRCondition<T> parse(String s) throws MCRParseException {
        s = s.replaceAll("\t", " ").replaceAll("\n", " ").replaceAll("\r", " ");

        if (s.trim().length() == 0 || s.equals("()")) {
            return defaultRule();
        }

        return parse(s, null);
    }

    private MCRCondition<T> parse(String s, List<String> l) throws MCRParseException {
        // initialize if start parsing
        if (l == null) {
            l = new ArrayList<>();
        }

        // a empty condition
        s = s.trim();
        if (s.equals("()")) {
            s = "(true)";
        }

        while (true) { 
            // replace all bracket expressions with $n
            while (s.charAt(0) == '(' && s.charAt(s.length() - 1) == ')'
                && s.substring(1, s.length() - 1).indexOf('(') < 0 && s.substring(1, s.length() - 1).indexOf(')') < 0) {
                s = s.trim().substring(1, s.length() - 1).trim();
            }

            // replace brackets in texts inside "..." with temporary strings 
            Matcher a = apostrophe.matcher(s); // find bracket pairs
            if (a.find()) {
                String clause = a.group();
                clause = clause.replaceAll("\\(", opening_bracket);
                clause = clause.replaceAll("\\)", closing_bracket);
                s = s.substring(0, a.start()) + clause + s.substring(a.end());
            }

            // find bracket pairs and replace text inside brackets with  @<number>@
            Matcher m = bracket.matcher(s);
            if (m.find()) {
                String clause = m.group();
                s = s.substring(0, m.start()) + "@<" + l.size() + ">@" + s.substring(m.end());
                l.add(extendClauses(clause, l));
            } else {
                break;
            }
        }

        // after replacing bracket pairs check for unmatched parenthis
        if ((s.indexOf('(') >= 0) ^ (s.indexOf(')') >= 0)) { // missing opening or closing bracket?
            throw new MCRParseException("Syntax error: missing bracket in \"" + s + "\"");
        }
        
        /* handle OR */
        Matcher m = or.matcher(s);
        int last = 0;
        MCROrCondition<T> orclause = new MCROrCondition<>();
        while (m.find()) {
            int l1 = m.start();
            if (last >= l1) {
                throw new MCRParseException("subclause of OR missing while parsing \"" + s + "\"");
            }
            MCRCondition<T> c = parse(extendClauses(s.substring(last, l1), l), l);
            last = m.end();
            orclause.addChild(c);
        }
        if (last != 0) {
            MCRCondition<T> c = parse(extendClauses(s.substring(last), l), l);
            orclause.addChild(c);
            return orclause;
        }

        /* handle AND */
        m = and.matcher(s);
        last = 0;
        MCRAndCondition<T> andclause = new MCRAndCondition<>();
        while (m.find()) {
            int l1 = m.start();
            if (last >= l1) {
                throw new MCRParseException("subclause of AND missing while parsing \"" + s + "\"");
            }
            MCRCondition<T> c = parse(extendClauses(s.substring(last, l1), l), l);
            last = m.end();
            andclause.addChild(c);
        }
        if (last != 0) {
            MCRCondition<T> c = parse(extendClauses(s.substring(last), l), l);
            andclause.addChild(c);
            return andclause;
        }

        /* handle NOT */
        s = s.trim();
        if (s.toLowerCase(Locale.ROOT).startsWith("not ")) {
            MCRCondition<T> inverse = parse(extendClauses(s.substring(4), l), l);
            return new MCRNotCondition<>(inverse);
        }

        // expands tokens with previously analysed expressions
        s = extendClauses(s, l);

        // recusion ONLY if parenthis (can) match
        if ((s.indexOf('(') >= 0) && (s.indexOf(')') >= 0)) {
            return parse(s, l);
        } else {
            // replace back brackets in apostrophe
            s = s.replaceAll(opening_bracket, "(");
            s = s.replaceAll(closing_bracket, ")");
            return parseSimpleCondition(s);
        }
    }

    protected MCRCondition<T> parseSimpleCondition(String s) throws MCRParseException {
        /* handle specific rules */
        if (s.equalsIgnoreCase("true")) {
            return new MCRTrueCondition<>();
        }

        if (s.equalsIgnoreCase("false")) {
            return new MCRFalseCondition<>();
        }

        throw new MCRParseException("Syntax error: " + s); // extendClauses(s,
        // l));
    }

    protected MCRCondition<T> parseSimpleCondition(Element element) throws MCRParseException {
        // <boolean operator="true|false" />
        String name;
        try {
            name = element.getAttributeValue("operator").toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            throw new MCRParseException("Syntax error: attribute operator not found");
        }

        if (name.equals("true")) {
            return new MCRTrueCondition<>();
        }

        if (name.equals("false")) {
            return new MCRFalseCondition<>();
        }

        throw new MCRParseException("Syntax error: <" + name + ">");
    }

    protected MCRCondition<T> defaultRule() {
        return new MCRTrueCondition<>();
    }

}
