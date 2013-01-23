package org.mycore.importer.mapping.condition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Element;
import org.mycore.parsers.bool.MCRBooleanClauseParser;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRFalseCondition;
import org.mycore.parsers.bool.MCRParseException;
import org.mycore.parsers.bool.MCRTrueCondition;

public class MCRImportParser extends MCRBooleanClauseParser {

    @Override
    protected MCRCondition parseSimpleCondition(Element e) throws MCRParseException {
        String name = e.getName();
        if (name.equals("boolean")) {
            return super.parseSimpleCondition(e);
        } else if (name.equals("condition")) {
            String operator = e.getAttributeValue("operator");
            String value1 = e.getAttributeValue("value1");
            String value2 = e.getAttributeValue("value2");
            return new MCRImportCondition(value1, operator, value2);
        }
        throw new MCRParseException("Not a valid name <" + name + ">");
    }

    private static Pattern pattern = Pattern.compile("([^ \t\r\n]+)\\s+([^ \t\r\n]+)\\s+([^ \t\r\n]+)");

    @Override
    protected MCRCondition parseSimpleCondition(String s) throws MCRParseException {
        /* handle specific rules */
        if (s.equalsIgnoreCase("false"))
            return new MCRFalseCondition();
        if (s.equalsIgnoreCase("true"))
            return new MCRTrueCondition();

        Matcher m = pattern.matcher(s);

        if (!m.find())
            throw new MCRParseException("Not a valid condition: " + s);

        String value1 = m.group(1);
        String operator = m.group(2);
        String value2 = m.group(3);

        if (value2.startsWith("\"") && value2.endsWith("\""))
            value2 = value2.substring(1, value2.length() - 1);

        return new MCRImportCondition(value1, operator, value2);
    }

}
