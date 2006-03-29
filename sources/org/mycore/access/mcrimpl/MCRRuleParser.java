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

package org.mycore.access.mcrimpl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jdom.Element;
import org.mycore.parsers.bool.MCRBooleanClauseParser;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRFalseCondition;
import org.mycore.parsers.bool.MCRParseException;
import org.mycore.parsers.bool.MCRTrueCondition;

class MCRRuleParser extends MCRBooleanClauseParser {
    MCRRuleParser() {
    }

    private static DateFormat dateformat = new SimpleDateFormat("dd.MM.yyyy");

    private static Date parseDate(String s, boolean dayafter) throws MCRParseException {
        try {
            long time = dateformat.parse(s).getTime();

            if (dayafter) {
                time += (1000 * 60 * 60 * 24);
            }

            return new Date(time);
        } catch (java.text.ParseException e) {
            throw new MCRParseException("unable to parse date " + s);
        }
    }

    protected MCRCondition parseSimpleCondition(Element e) throws MCRParseException {
        String name = e.getName();
        if (name.equals("boolean")) {
        	return super.parseSimpleCondition(e);
        } else if (name.equals("condition")) {
            String field = e.getAttributeValue("field").toLowerCase().trim();
            String operator = e.getAttributeValue("operator").trim();
            String value = e.getAttributeValue("value").trim();
            boolean not = "!=".equals(operator);

            if (field.equals("group")) {
            	return new MCRGroupClause(value, not);
            } else if (field.equals("user")) {
            	return new MCRUserClause(value, not);
            }else if (field.equals("ip")) {
            	return new MCRIPClause(value);
            }else if (field.equals("date")) {
            	if(operator.equals("<")) {
            		return new MCRDateBeforeClause(parseDate(value, false));
            	}else if(operator.equals("<=")) {
            		return new MCRDateBeforeClause(parseDate(value, true));
            	}else if(operator.equals(">")) {
            		return new MCRDateAfterClause(parseDate(value, true));
            	}else if(operator.equals(">=")) {
            		return new MCRDateAfterClause(parseDate(value, false));
            	}
            	else {
            		throw new MCRParseException("Not a valid operator <" + operator + ">");
            	}
            }else {
            	throw new MCRParseException("Not a valid condition field <" + field + ">");
            }
        }else{
            throw new MCRParseException("Not a valid name <" + name + ">");
        }  	
    }

    protected MCRCondition parseSimpleCondition(String s) throws MCRParseException {
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
            } else if(s.startsWith("=")) {
                return new MCRGroupClause(s.substring(1).trim(), false);
            } else throw new MCRParseException("syntax error: " + s);
        }

        if (s.startsWith("user")) {
            s = s.substring(4).trim();
            if (s.startsWith("!=")) {
                return new MCRUserClause(s.substring(2).trim(), true);
            } else if(s.startsWith("=")) {
                return new MCRUserClause(s.substring(1).trim(), false);
            } else throw new MCRParseException("syntax error: " + s);
        }

        if (s.startsWith("ip ")) {
            return new MCRIPClause(s.substring(3).trim());
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
            } else throw new MCRParseException("syntax error: " + s);
        }

        throw new MCRParseException("syntax error: " + s);
    }
};
