package org.mycore.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Hashtable;

import org.junit.Test;
import org.mycore.common.MCRTextResolver.CircularDependencyExecption;
import org.mycore.common.MCRTextResolver.ResolveDepth;
import org.mycore.common.MCRTextResolver.Term;

public class MCRTextResolverTest extends MCRTestCase {

    @Test
    public void variables() throws Exception {
        Hashtable<String, String> variablesTable = new Hashtable<String, String>();
        variablesTable.put("f1", "v1");
        variablesTable.put("f2", "v2");
        variablesTable.put("f3", "v3");
        variablesTable.put("f4", "v4 - {f1}");
        variablesTable.put("f5", "[{f1}[_{e1}]]");
        variablesTable.put("f6", "[[[[{f3}]]]]");

        variablesTable.put("num", "10");
        variablesTable.put("add", "5");
        variablesTable.put("x_10_5", "value1");
        variablesTable.put("x_10", "value2");

        MCRTextResolver resolver = new MCRTextResolver(variablesTable);

        // some simple variables tests
        assertEquals("v1", resolver.resolve("{f1}"));
        assertEquals("v2 & v3", resolver.resolve("{f2} & {f3}"));
        // internal variables
        assertEquals("v4 - v1", resolver.resolve("{f4}"));
        // conditions
        assertEquals("v1_v2", resolver.resolve("{f1}[_{f2}][_{e1}]"));
        // internal conditions
        assertEquals("", resolver.resolve("{f5}"));
        // advanced condition tests
        assertEquals("v3", resolver.resolve("{f6}"));
        // escaping char test
        assertEquals("[{v1}] \\", resolver.resolve("\\[\\{{f1}\\}\\] \\\\"));
        // resolving variables in an other variable
        assertEquals("value1", resolver.resolve("{x_{num}[_{add}]}"));
        assertEquals("value2", resolver.resolve("{x_{num}[_{add2}]}"));
        // uncompleted variable
        assertEquals("{f1 v2", resolver.resolve("{f1 [{f2}]"));
    }

    @Test
    public void addRemove() throws Exception {
        Hashtable<String, String> variablesTable = new Hashtable<String, String>();
        variablesTable.put("f1", "v1");
        variablesTable.put("f2", "v2");
        variablesTable.put("f3", "v3");
        MCRTextResolver resolver = new MCRTextResolver(variablesTable);
        resolver.addVariable("f4", "v4");
        assertEquals("v1, v2, v3, v4", resolver.resolve("{f1}, {f2}, {f3}, {f4}"));
        resolver.removeVariable("f2");
        resolver.removeVariable("f3");
        assertEquals("v1, v4", resolver.resolve("[{f1}][, {f2}][, {f3}][, {f4}]"));
        assertEquals(true, resolver.containsVariable("f1"));
        assertEquals(false, resolver.containsVariable("f2"));
    }

    @Test
    public void resolveDepth() throws Exception {
        MCRTextResolver resolver = new MCRTextResolver();
        resolver.addVariable("var1", "test1 & [{var2}]");
        resolver.addVariable("var2", "test2");
        assertEquals("test1 & test2", resolver.resolve("{var1}"));
        resolver.setResolveDepth(ResolveDepth.NoVariables);
        assertEquals("test1 & [{var2}]", resolver.resolve("{var1}"));
        assertEquals(ResolveDepth.NoVariables, resolver.getResolveDepth());
    }

    @Test
    public void terms() throws Exception {
        MCRTextResolver resolver = new MCRTextResolver();
        resolver.registerTerm(UppercaseTerm.class);
        resolver.setRetainText(false);
        resolver.addVariable("var1", "test");
        assertEquals("Das ist ein TEST.", resolver.resolve("Das ist ein${ {var1}}$."));
        resolver.unregisterTerm(UppercaseTerm.class);
        assertEquals("Das ist ein$$.", resolver.resolve("Das ist ein${ {var1}}$."));
    }

    @Test
    public void retainText() {
        MCRTextResolver resolver = new MCRTextResolver();
        assertEquals("Hello {variable}", resolver.resolve("Hello {variable}"));
        resolver.setRetainText(false);
        assertEquals("Hello ", resolver.resolve("Hello {variable}"));
    }

    @Test
    public void circularDependency() {
        MCRTextResolver resolver = new MCRTextResolver();
        resolver.addVariable("a", "{b}");
        resolver.addVariable("b", "{c}");
        resolver.addVariable("c", "{a}");
        try {
            resolver.resolve("{a}");
        } catch (CircularDependencyExecption cde) {
            return;
        }
        assertFalse("No circular dependency occurred", true);
    }

    private static class UppercaseTerm extends Term {

        public UppercaseTerm(MCRTextResolver textResolver) {
            super(textResolver);
        }

        @Override
        public String getEndEnclosingString() {
            return "}$";
        }

        @Override
        public String getStartEnclosingString() {
            return "${";
        }

        @Override
        protected boolean resolveInternal(String text, int pos) {
            if (text.startsWith(getEndEnclosingString(), pos)) {
                String value = termBuffer.toString().toUpperCase();
                termBuffer = new StringBuffer(value);
                return true;
            }
            char c = text.charAt(pos);
            termBuffer.append(c);
            return false;
        }
    }
}
