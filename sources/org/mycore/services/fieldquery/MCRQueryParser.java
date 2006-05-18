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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.datamodel.metadata.MCRMetaISO8601Date;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRBooleanClauseParser;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRNotCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.parsers.bool.MCRParseException;

/**
 * Parses query conditions for use in MCRSearcher.
 * 
 * @see MCRSearcher
 * 
 * @author Frank Lützenkirchen
 */
public class MCRQueryParser extends MCRBooleanClauseParser {

    private final static Logger LOGGER = Logger.getLogger(MCRQueryParser.class);

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

        return buildConditions(field, opera, value);
    }

    /**
     * Builds a new MCRCondition from parsed elements
     * 
     * @param field
     *            one or more field names, separated by comma
     * @param oper
     *            the condition operator
     * @param value
     *            the condition value
     * @return
     */
    private MCRCondition buildConditions(String field, String oper, String value) {
        if (field.indexOf(",") == -1)
            return buildCondition(field, oper, value);
        else // Multiple fields in one condition, combine with OR
        {
            StringTokenizer st = new StringTokenizer(field, ", ");
            MCROrCondition oc = new MCROrCondition();
            while (st.hasMoreTokens())
                oc.addChild(buildCondition(st.nextToken(), oper, value));
            return oc;
        }
    }

    /**
     * Builds a new MCRQueryCondition
     * 
     * @param field
     *            the name of the search field
     * @param oper
     *            the condition operator
     * @param value
     *            the condition value
     * @return
     */
    private MCRQueryCondition buildCondition(String field, String oper, String value) {
        MCRFieldDef def = MCRFieldDef.getDef(field);
        if (def == null)
            throw new MCRParseException("Field not defined: <" + field + ">");
        return new MCRQueryCondition(def, oper, value);
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

        return buildConditions(field, operator, value);
    }

    public MCRCondition parse(Element condition) throws MCRParseException {
        MCRCondition cond = super.parse(condition);
        return normalizeCondition(cond);
    }

    public MCRCondition parse(String s) throws MCRParseException {
        MCRCondition cond = super.parse(s);
        return normalizeCondition(cond);
    }

    /**
     * Normalizes a parsed query condition. AND/OR conditions that just have one
     * child will be replaced with that child. Conditions that use the operator
     * "contains" will be splitted into multiple simpler conditions if the
     * condition value contains phrases surrounded by '...' or wildcard search
     * with * or ?.
     */
    private MCRCondition normalizeCondition(MCRCondition cond) {
        if (cond instanceof MCRAndCondition) {
            MCRAndCondition ac = (MCRAndCondition) cond;
            List children = ac.getChildren();
            if (children != null)
                for (int i = 0; i < children.size(); i++)
                    children.set(i, normalizeCondition((MCRCondition) (children.get(i))));
            return (children.size() == 1 ? (MCRCondition) (children.get(0)) : ac);
        } else if (cond instanceof MCROrCondition) {
            MCROrCondition oc = (MCROrCondition) cond;
            List children = oc.getChildren();
            if (children != null)
                for (int i = 0; i < children.size(); i++)
                    children.set(i, normalizeCondition((MCRCondition) (children.get(i))));
            return (children.size() == 1 ? (MCRCondition) (children.get(0)) : oc);
        } else if (cond instanceof MCRNotCondition) {
            MCRNotCondition nc = (MCRNotCondition) cond;
            if (nc.getChild() != null)
                return new MCRNotCondition(normalizeCondition(nc.getChild()));
            else
                return nc;
        } else if (cond instanceof MCRQueryCondition) {
            MCRQueryCondition qc = (MCRQueryCondition) cond;

            // Normalize values in date conditions
            if (qc.getField().getDataType().equals("date")) {
                try {
                    MCRMetaISO8601Date iDate = new MCRMetaISO8601Date();
                    iDate.setDate(qc.getValue());
                    String sDate = iDate.getISOString().substring(0, 10);
                    return new MCRQueryCondition(qc.getField(), qc.getOperator(), sDate);
                } catch (Exception ex) {
                    LOGGER.debug(ex);
                    return qc;
                }
            }

            if (!qc.getOperator().equals("contains"))
                return qc;

            // Normalize value when contains operator is used
            List values = new ArrayList();

            String phrase = null;
            StringTokenizer st = new StringTokenizer(qc.getValue(), " ");
            while (st.hasMoreTokens()) {
                String value = st.nextToken();
                if ((phrase != null)) // we are within phrase
                {
                    if (value.endsWith("'")) // end of phrase
                    {
                        value = phrase + " " + value;
                        values.add(value);
                        phrase = null;
                    } else // in middle of phrase
                    {
                        phrase = phrase + " " + value;
                    }
                } else if (value.startsWith("'")) // begin of phrase
                {
                    if (value.endsWith("'")) // one-word phrase
                    {
                        values.add(value.substring(1, value.length() - 1));
                    } else {
                        phrase = value;
                    }
                } else
                    values.add(value);
            }

            MCRAndCondition ac = new MCRAndCondition();
            for (int i = 0; i < values.size(); i++) {
                String value = (String) (values.get(i));
                if (value.startsWith("'")) // phrase
                    ac.addChild(new MCRQueryCondition(qc.getField(), "phrase", value.substring(1, value.length() - 1)));
                else if ((value.indexOf("*") >= 0) || (value.indexOf("?") >= 0)) // like
                    ac.addChild(new MCRQueryCondition(qc.getField(), "like", value));
                else if (value.startsWith("-")) // -word means "NOT word"
                {
                    MCRCondition subCond = new MCRQueryCondition(qc.getField(), "contains", value.substring(1));
                    ac.addChild(new MCRNotCondition(subCond));
                } else
                    ac.addChild(new MCRQueryCondition(qc.getField(), "contains", value));
            }

            if (values.size() == 1)
                return (MCRCondition) (ac.getChildren().get(0));
            else
                return ac;
        } else
            return cond;
    }

    /** Used for input validation in editor search form */
    public static boolean validateQueryExpression(String query) {
        try {
            MCRCondition cond = new MCRQueryParser().parse(query);
            return (cond != null);
        } catch (Exception ex) {
            return false;
        }
    }
}
