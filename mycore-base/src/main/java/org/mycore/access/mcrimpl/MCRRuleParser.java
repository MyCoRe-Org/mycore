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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import org.jdom2.Element;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.parsers.bool.MCRBooleanClauseParser;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRFalseCondition;
import org.mycore.parsers.bool.MCRIPCondition;
import org.mycore.parsers.bool.MCRParseException;
import org.mycore.parsers.bool.MCRTrueCondition;

public class MCRRuleParser extends MCRBooleanClauseParser {
    protected MCRRuleParser() {
    }

    private static Date parseDate(String s, boolean dayafter) throws MCRParseException {
        long time;
        try {
            time = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(s).getTime();
        } catch (ParseException e) {
            try {
                time = new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT).parse(s).getTime();
            } catch (ParseException e1) {
                MCRParseException parseException = new MCRParseException("unable to parse date " + s, e1);
                parseException.addSuppressed(e);
                throw parseException;
            }
        }

        if (dayafter) {
            time += 1000 * 60 * 60 * 24;
        }

        return new Date(time);
    }

    @Override
    protected MCRCondition<?> parseSimpleCondition(Element e) throws MCRParseException {
        String name = e.getName();
        switch (name) {
            case "boolean" -> {
                return super.parseSimpleCondition(e);
            }
            case "condition" -> {
                MCRCondition<?> condition = parseElement(e);
                if (condition == null) {
                    throw new MCRParseException("Not a valid condition field <" + e.getAttributeValue("field") + ">");
                }
                return condition;
            }
            default -> throw new MCRParseException("Not a valid name <" + e.getName() + ">");
        }
    }

    @Override
    protected MCRCondition<?> parseSimpleCondition(String s) throws MCRParseException {
        MCRCondition<?> condition = parseString(s);
        if (condition == null) {
            throw new MCRParseException("syntax error: " + s);
        }
        return condition;
    }

    protected MCRCondition<?> parseElement(Element e) {
        String field = e.getAttributeValue("field").toLowerCase(Locale.ROOT).trim();
        String operator = e.getAttributeValue("operator").trim();
        String value = e.getAttributeValue("value").trim();
        boolean not = Objects.equals(operator, "!=");

        return switch (field) {
            case "group" -> new MCRGroupClause(value, not);
            case "user" -> new MCRUserClause(value, not);
            case "ip" -> getIPClause(value);
            case "date" -> switch (operator) {
                case "<" -> new MCRDateBeforeClause(parseDate(value, false));
                case "<=" -> new MCRDateBeforeClause(parseDate(value, true));
                case ">" -> new MCRDateAfterClause(parseDate(value, true));
                case ">=" -> new MCRDateAfterClause(parseDate(value, false));
                default -> throw new MCRParseException("Not a valid operator <" + operator + ">");
            };
            default -> null;
        };
    }

    private MCRCondition<MCRAccessData> getIPClause(String value) {
        MCRIPCondition ipCond = MCRConfiguration2.getInstanceOfOrThrow(MCRIPCondition.class, "MCR.RuleParser.ip");
        ipCond.set(value);
        return ipCond;
    }

    protected MCRCondition<?> parseString(String s) {
        /* handle specific rules */
        String parsedstring = s;
        if (parsedstring.equalsIgnoreCase("false")) {
            return new MCRFalseCondition<>();
        } else if (parsedstring.equalsIgnoreCase("true")) {
            return new MCRTrueCondition<>();
        } else {

            if (parsedstring.startsWith("group")) {
                parsedstring = parsedstring.substring(5).trim();
                if (parsedstring.startsWith("!=")) {
                    return new MCRGroupClause(parsedstring.substring(2).trim(), true);
                } else if (parsedstring.startsWith("=")) {
                    return new MCRGroupClause(parsedstring.substring(1).trim(), false);
                }
                throw new MCRParseException("syntax error for group: " + parsedstring);
            }

            if (parsedstring.startsWith("user")) {
                parsedstring = parsedstring.substring(4).trim();
                if (parsedstring.startsWith("!=")) {
                    return new MCRUserClause(parsedstring.substring(2).trim(), true);
                } else if (parsedstring.startsWith("=")) {
                    return new MCRUserClause(parsedstring.substring(1).trim(), false);
                }
                throw new MCRParseException("syntax error for user: " + parsedstring);
            }

            if (parsedstring.startsWith("ip ")) {
                return getIPClause(parsedstring.substring(3).trim());
            }

            if (parsedstring.startsWith("date ")) {
                parsedstring = parsedstring.substring(5).trim();
                return handleDateCondition(parsedstring);
            }
        }
        return null;
    }

    private MCRCondition<?> handleDateCondition(String s) {
        if (s.startsWith(">=")) {
            return new MCRDateAfterClause(parseDate(s.substring(2).trim(), false));
        } else if (s.startsWith("<=")) {
            return new MCRDateBeforeClause(parseDate(s.substring(2).trim(), true));
        } else if (s.startsWith(">")) {
            return new MCRDateAfterClause(parseDate(s.substring(1).trim(), true));
        } else if (s.startsWith("<")) {
            return new MCRDateBeforeClause(parseDate(s.substring(1).trim(), false));
        }
        throw new MCRParseException("syntax error: " + s);
    }
}
