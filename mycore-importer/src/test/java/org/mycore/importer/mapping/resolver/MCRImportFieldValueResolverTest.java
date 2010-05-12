package org.mycore.importer.mapping.resolver;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.importer.MCRImportField;

public class MCRImportFieldValueResolverTest extends MCRTestCase {

    @Test
    public void constructor() {
        // create test fields
        MCRImportField root = new MCRImportField("root", null);
        MCRImportField p1 = new MCRImportField("p", "parent 1");
        MCRImportField p2 = new MCRImportField("p", "parent 2");
        MCRImportField c1 = new MCRImportField("c1", "child 1");
        MCRImportField c2 = new MCRImportField("c1", "child 2");
        MCRImportField c3 = new MCRImportField("c2", "child 3");

        // create structure
        root.addField(p1);
        root.addField(p2);
        p1.addField(c1);
        p2.addField(c2);
        p2.addField(c3);

        // do tests
        List<MCRImportField> fieldList = new ArrayList<MCRImportField>();
        fieldList.add(p1);
        MCRImportFieldValueResolver resolver = new MCRImportFieldValueResolver(fieldList);
        assertEquals("child 1", resolver.resolveFields("{root.p.c1}"));
        fieldList.remove(p1);
        fieldList.add(p2);
        resolver = new MCRImportFieldValueResolver(fieldList);
        assertEquals("child 2", resolver.resolveFields("{root.p.c1}"));
        assertEquals("child 3", resolver.resolveFields("{root.p.c2}"));
    }

    @Test
    public void resolveFields() {
        List<MCRImportField> fieldList = new ArrayList<MCRImportField>();
        MCRImportField field1 = new MCRImportField("f1", "v1");
        MCRImportField field2 = new MCRImportField("f2", "v2");
        MCRImportField field3 = new MCRImportField("f3", "v3");
        MCRImportField field4 = new MCRImportField("f4", "v4 - {f1}");

        MCRImportField fieldNum = new MCRImportField("num", "10");
        MCRImportField fieldAdd = new MCRImportField("add", "5");
        MCRImportField fieldX_10_5 = new MCRImportField("x_10_5", "value1");
        MCRImportField fieldX_10 = new MCRImportField("x_10", "value2");

        fieldList.add(field1);
        fieldList.add(field2);
        fieldList.add(field3);
        fieldList.add(field4);

        fieldList.add(fieldNum);
        fieldList.add(fieldAdd);
        fieldList.add(fieldX_10_5);
        fieldList.add(fieldX_10);

        MCRImportFieldValueResolver resolver = new MCRImportFieldValueResolver(fieldList);

        assertEquals("v1", resolver.resolveFields("{f1}"));
        assertEquals("v2 & v3", resolver.resolveFields("{f2} & {f3}"));
        assertEquals("v4 - {f1}", resolver.resolveFields("{f4}"));
        assertEquals("v1_v2", resolver.resolveFields("{f1}[_{f2}][_{e1}]"));

        assertEquals("[{v1}] \\", resolver.resolveFields("\\[\\{{f1}\\}\\] \\\\"));

        assertEquals("value1", resolver.resolveFields("{x_{num}[_{add}]}"));
        assertEquals("value2", resolver.resolveFields("{x_{num}[_{add2}]}"));

        assertEquals(0, resolver.getNotUsedFields().size());
        resolver = new MCRImportFieldValueResolver(fieldList);
        resolver.resolveFields("{f1}, {add}, {notInFieldList}");
        // contains f2, f3, f4, num, x_10_5, x_10
        assertEquals(6, resolver.getNotUsedFields().size());
    }
    
}
