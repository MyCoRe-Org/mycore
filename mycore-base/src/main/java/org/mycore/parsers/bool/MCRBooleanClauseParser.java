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

    /**
     * This both strings are for temporary bracket substitution in case of brackets 
     * in a text string in a condition like 'title contains "foo (and bar)".
     */
    private static String opening_bracket = "%%%%%%%%%%";

    private static String closing_bracket = "##########";

    private static String extendClauses(final String s, final List<String> l) {
        String sintern = s;
        while (true) {
            Matcher m = bracket_marker.matcher(sintern);

            if (m.find()) {
                String c = m.group();
                String clause = l.get(Integer.parseInt(m.group(1)));
                sintern = sintern.replaceAll(c, clause);
            } else {
                break;
            }
        }

        return sintern;
    }

    /**
     * Parse a complex or simple condition in XML format and put it in an condition object.
     * 
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
                Element child = condition.getChildren().getFirst();
                return new MCRNotCondition<>(parse(child));
            } else if (operator.equalsIgnoreCase("and") || operator.equalsIgnoreCase("or")) {
                List<Element> children = condition.getChildren();
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
     *
     * @param s a MyCoRe condition object in String format
     * @return a MyCoRe condition object in the MCRCondition format
     */
    public MCRCondition<T> parse(String s) throws MCRParseException {
        String cleanedString = s
                .replaceAll("\t", " ")
                .replaceAll("\n", " ")
                .replaceAll("\r", " ");

        if (cleanedString.trim().length() == 0 || cleanedString.equals("()")) {
            return defaultRule();
        }
        return parse(cleanedString, null);
    }
    private MCRCondition<T> parse(String s, List<String> l) throws MCRParseException {
        // initialize if start parsing
        List<String> list;
        if (l == null) {
            list = new ArrayList<>();
        }else{
            list= l;
        }

        // a empty condition
        String stringTrimmed = s.trim();
        if (stringTrimmed.equals("()")) {
            stringTrimmed = "(true)";
        }

        //        StringTrimmed,list
        while (true) {
            stringTrimmed = replaceAllBracketExpression(stringTrimmed);

            // find bracket pairs
            stringTrimmed = findBracketPairs(stringTrimmed);

            // find bracket pairs and replace text inside brackets with  @<number>@
            Matcher m = bracket.matcher(stringTrimmed);
            if (m.find()) {
                String clause = m.group();
                stringTrimmed = stringTrimmed.substring(0, m.start())
                        + "@<" + list.size() + ">@" + stringTrimmed.substring(m.end());
                list.add(extendClauses(clause, list));
            } else {
                break;
            }
        }


        // after replacing bracket pairs check for unmatched parenthis
        if ((stringTrimmed.indexOf('(') >= 0) ^ (stringTrimmed.indexOf(')') >= 0)) {
            // missing opening or closing bracket?
            throw new MCRParseException("Syntax error: missing bracket in \"" + stringTrimmed + "\"");
        }

        //handle and or

        /* handle OR */
        MCROrCondition<T> orClause = new MCROrCondition<>();
        orClause = (MCROrCondition<T>) handleAndORClause(stringTrimmed, list, orClause,or);
        if (orClause != null) {
            return orClause;
        }

        /* handle AND */
        MCRAndCondition<T> andClause = new MCRAndCondition<>();
        andClause = (MCRAndCondition<T>) handleAndORClause(stringTrimmed, list, andClause,and);
        if (andClause != null) {
            return andClause;
        }
        /* handle NOT */
        stringTrimmed = stringTrimmed.trim();
        if (stringTrimmed.toLowerCase(Locale.ROOT).startsWith("not ")) {
            MCRCondition<T> inverse = parse(extendClauses(stringTrimmed.substring(4), list), list);
            return new MCRNotCondition<>(inverse);
        }

        // expands tokens with previously analysed expressions
        stringTrimmed = extendClauses(stringTrimmed, list);
        // recusion ONLY if parenthis (can) match
        return handleBrackets(stringTrimmed, list);
    }

    private String findBracketPairs(String string) {
        Matcher a = apostrophe.matcher(string); // find bracket pairs
        String stringTrimmed = string;
        if (a.find()) {
            String clause = a.group();
            clause = clause.replaceAll("\\(", opening_bracket);
            clause = clause.replaceAll("\\)", closing_bracket);
            stringTrimmed = string.substring(0, a.start()) + clause + string.substring(a.end());
        }
        return stringTrimmed;
    }

    private MCRCondition<T> handleBrackets(String string, List<String> list) {
        MCRCondition<T> mcrCondition;
        // recusion ONLY if parenthis (can) match
        if ((string.indexOf('(') >= 0) && (string.indexOf(')') >= 0)) {
            mcrCondition = parse(string, list);
        } else {
            // replace back brackets in apostrophe
            String s = string.replaceAll(opening_bracket, "(");
            s = s.replaceAll(closing_bracket, ")");
            mcrCondition = parseSimpleCondition(s);
        }
        return mcrCondition;
    }

    private String replaceAllBracketExpression(String str) {
        // replace all bracket expressions with $n
        String stringTrimmed = str;
        while (isBracketExpression(stringTrimmed)) {
            stringTrimmed = stringTrimmed.trim().substring(1, stringTrimmed.length() - 1).trim();
        }
        return stringTrimmed;
    }

    private boolean isBracketExpression(String str) {
        return str.charAt(0) == '(' &&
            str.charAt(str.length() - 1) == ')' &&
            str.substring(1, str.length() - 1).indexOf('(') < 0 &&
            str.substring(1, str.length() - 1).indexOf(')') < 0;
    }


    private MCRSetCondition handleAndORClause(String stringTrimmed, List<String> list, MCRSetCondition condition,
                                              Pattern pattern) {
        Matcher m = pattern.matcher(stringTrimmed);
        int last = 0;

        while (m.find()) {
            int l1 = m.start();
            if (last >= l1) {
                throw new MCRParseException("subclause of AND missing while parsing \"" + stringTrimmed + "\"");
            }
            MCRCondition<T> c = parse(extendClauses(stringTrimmed.substring(last, l1), list), list);
            last = m.end();
            condition.addChild(c);
        }
        if (last != 0) {
            MCRCondition<T> c = parse(extendClauses(stringTrimmed.substring(last), list), list);
            condition.addChild(c);
            return condition;
        }
        return null;
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
