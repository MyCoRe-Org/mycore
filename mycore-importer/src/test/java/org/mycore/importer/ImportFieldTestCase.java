package org.mycore.importer;

import java.util.ArrayList;
import java.util.List;

import org.mycore.common.MCRTestCase;
import org.mycore.importer.mapping.resolver.MCRImportFieldValueResolver;

public class ImportFieldTestCase extends MCRTestCase {

    public void testResolver() throws Exception {
        List<MCRImportField> fieldList = new ArrayList<MCRImportField>();
        MCRImportField field1 = new MCRImportField("f1", "v1");
        MCRImportField field2 = new MCRImportField("f2", "v2");
        MCRImportField field3 = new MCRImportField("f3", "v3");
        MCRImportField field4 = new MCRImportField("f4", "v4 - {f1}");
        MCRImportField field5 = new MCRImportField("f5", "[{f1}[_{e1}]]");
        MCRImportField field6 = new MCRImportField("f6", "[[[[{f3}]]]]");

        MCRImportField fieldNum = new MCRImportField("num", "10");
        MCRImportField fieldAdd = new MCRImportField("add", "5");
        MCRImportField fieldX_10_5 = new MCRImportField("x_10_5", "value1");
        MCRImportField fieldX_10 = new MCRImportField("x_10", "value2");

        fieldList.add(field1);
        fieldList.add(field2);
        fieldList.add(field3);
        fieldList.add(field4);
        fieldList.add(field5);
        fieldList.add(field6);

        fieldList.add(fieldNum);
        fieldList.add(fieldAdd);
        fieldList.add(fieldX_10_5);
        fieldList.add(fieldX_10);

        MCRImportFieldValueResolver resolver = new MCRImportFieldValueResolver(fieldList);

        assertEquals("v1", resolver.resolveFields("{f1}"));
        assertEquals("v2 & v3", resolver.resolveFields("{f2} & {f3}"));
        assertEquals("v4 - v1", resolver.resolveFields("{f4}"));

        assertEquals("v1_v2", resolver.resolveFields("{f1}[_{f2}][_{e1}]"));
        assertEquals("", resolver.resolveFields("{f5}"));
        assertEquals("v3", resolver.resolveFields("{f6}"));

        assertEquals("[{v1}] \\", resolver.resolveFields("\\[\\{{f1}\\}\\] \\\\"));

        assertEquals("value1", resolver.resolveFields("{x_{num}[_{add}]}"));
        assertEquals("value2", resolver.resolveFields("{x_{num}[_{add2}]}"));

        assertEquals(0, resolver.getNotUsedFields().size());
        resolver = new MCRImportFieldValueResolver(fieldList);
        resolver.resolveFields("{f1}, {f6}, {add}, {notInFieldList}");
        // contains f1, f6, f3 and add
        assertEquals(6, resolver.getNotUsedFields().size());
        
    }
}