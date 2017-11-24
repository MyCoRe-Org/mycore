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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.jdom2.Element;
import org.mycore.common.config.MCRConfiguration;
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
        } catch (java.text.ParseException e) {
            try {
                time = new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT).parse(s).getTime();
            } catch (ParseException e1) {
                throw new MCRParseException("unable to parse date " + s);
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
            case "boolean":
                return super.parseSimpleCondition(e);
            case "condition":
                MCRCondition<?> condition = parseElement(e);
                if (condition == null) {
                    throw new MCRParseException("Not a valid condition field <" + e.getAttributeValue("field") + ">");
                }
                return condition;
            default:
                throw new MCRParseException("Not a valid name <" + e.getName() + ">");
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
        boolean not = "!=".equals(operator);

        switch (field) {
            case "group":
                return new MCRGroupClause(value, not);
            case "user":
                return new MCRUserClause(value, not);
            case "ip":
                return getIPClause(value);
            case "date":
                switch (operator) {
                    case "<":
                        return new MCRDateBeforeClause(parseDate(value, false));
                    case "<=":
                        return new MCRDateBeforeClause(parseDate(value, true));
                    case ">":
                        return new MCRDateAfterClause(parseDate(value, true));
                    case ">=":
                        return new MCRDateAfterClause(parseDate(value, false));
                    default:
                        throw new MCRParseException("Not a valid operator <" + operator + ">");
                }
            default:
                return null;
        }
    }

    private MCRCondition<MCRAccessData> getIPClause(String value) {
        MCRIPCondition ipCond = MCRConfiguration.instance().getInstanceOf("MCR.RuleParser.ip",
            MCRIPClause.class.getName());
        ipCond.set(value);
        return ipCond;
    }

    protected MCRCondition<?> parseString(String s) {
        /* handle specific rules */
        if (s.equalsIgnoreCase("false")) {
            return new MCRFalseCondition();
        }

        if (s.equalsIgnoreCase("true")) {
            return new MCRTrueCondition();
        }

        if (s.startsWith("group")) {
            s = s.substring(5).trim();
            if (s.startsWith("!=")) {
                return new MCRGroupClause(s.substring(2).trim(), true);
            } else if (s.startsWith("=")) {
                return new MCRGroupClause(s.substring(1).trim(), false);
            } else {
                throw new MCRParseException("syntax error: " + s);
            }
        }

        if (s.startsWith("user")) {
            s = s.substring(4).trim();
            if (s.startsWith("!=")) {
                return new MCRUserClause(s.substring(2).trim(), true);
            } else if (s.startsWith("=")) {
                return new MCRUserClause(s.substring(1).trim(), false);
            } else {
                throw new MCRParseException("syntax error: " + s);
            }
        }

        if (s.startsWith("ip ")) {
            return getIPClause(s.substring(3).trim());
        }

        if (s.startsWith("date ")) {
            s = s.substring(5).trim();
            if (s.startsWith(">=")) {
                return new MCRDateAfterClause(parseDate(s.substring(2).trim(), false));
            } else if (s.startsWith("<=")) {
                return new MCRDateBeforeClause(parseDate(s.substring(2).trim(), true));
            } else if (s.startsWith(">")) {
                return new MCRDateAfterClause(parseDate(s.substring(1).trim(), true));
            } else if (s.startsWith("<")) {
                return new MCRDateBeforeClause(parseDate(s.substring(1).trim(), false));
            } else {
                throw new MCRParseException("syntax error: " + s);
            }
        }
        return null;
    }
}
