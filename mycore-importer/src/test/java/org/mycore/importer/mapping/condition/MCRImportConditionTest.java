package org.mycore.importer.mapping.condition;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.importer.MCRImportField;
import org.mycore.importer.mapping.resolver.MCRImportFieldValueResolver;

public class MCRImportConditionTest extends MCRTestCase {

    private MCRImportFieldValueResolver res;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MCRImportField field = new MCRImportField("f1", "v1");
        MCRImportField field2 = new MCRImportField("f2", "5");
        MCRImportField field3 = new MCRImportField("f3", "7.2");
        ArrayList<MCRImportField> fieldList = new ArrayList<MCRImportField>();
        fieldList.add(field);
        fieldList.add(field2);
        fieldList.add(field3);
        res = new MCRImportFieldValueResolver(fieldList);
    }

    @Test
    public void evaluate() {
        // equals
        MCRImportCondition cond = new MCRImportCondition("v1", "=", "{f1}");
        assertEquals(true, cond.evaluate(res));
        // not equal to
        cond = new MCRImportCondition("v1", "!=", "{f1}");
        assertEquals(false, cond.evaluate(res));
        // contains
        cond = new MCRImportCondition("test v1 test", "contains", "{f1}");
        assertEquals(true, cond.evaluate(res));
        // greater than
        cond = new MCRImportCondition("1", ">", "{f2}");
        assertEquals(false, cond.evaluate(res));
        // less than
        cond = new MCRImportCondition("1", "<", "{f2}");
        assertEquals(true, cond.evaluate(res));
        // greater or equal
        cond = new MCRImportCondition("5", ">=", "{f2}");
        assertEquals(true, cond.evaluate(res));
        // lower or equal
        cond = new MCRImportCondition("6", "<=", "{f2}");
        assertEquals(false, cond.evaluate(res));
        // two fields
        cond = new MCRImportCondition("{f2}", ">=", "{f3}");
        assertEquals(false, cond.evaluate(res));
    }

}
