package org.mycore.importer.convert;

import static org.junit.Assert.*;

import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.importer.mapping.condition.MCRImportCondition;
import org.mycore.importer.mapping.condition.MCRImportParser;
import org.mycore.parsers.bool.MCRCondition;

public class MCRImportParserTest extends MCRTestCase {

    @Test
    public void parseSimpleConditionElement() {
        MCRImportParser parser = new MCRImportParser();
        Element condition = new Element("condition");
        condition.setAttribute("value1", "20");
        condition.setAttribute("operator", ">");
        condition.setAttribute("value2", "25");
        MCRCondition mcrCondition = parser.parse(condition);
        if(!(mcrCondition instanceof MCRImportCondition))
            fail();
    }

    @Test
    public void parseSimpleConditionString() {
        MCRImportParser parser = new MCRImportParser();
        MCRCondition mcrCondition = parser.parse("(value = value)");
        if(!(mcrCondition instanceof MCRImportCondition))
            fail();
    }
}
