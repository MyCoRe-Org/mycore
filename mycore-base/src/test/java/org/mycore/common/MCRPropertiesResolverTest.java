package org.mycore.common;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;
import org.mycore.common.config.MCRConfiguration;

public class MCRPropertiesResolverTest {

    @Test
    public void resolve() {
        MCRConfiguration.instance().set("Sample.basedir", "/home/user/base");
        MCRConfiguration.instance().set("Sample.subdir", "%Sample.basedir%/subdir");
        MCRConfiguration.instance().set("Sample.file", "%Sample.subdir%/file.txt");
        MCRPropertiesResolver resolver = new MCRPropertiesResolver(MCRConfiguration.instance().getPropertiesMap());
        assertEquals("/home/user/base", resolver.resolve("%Sample.basedir%"));
        assertEquals("/home/user/base/subdir", resolver.resolve("%Sample.subdir%"));
        assertEquals("/home/user/base/subdir/file.txt", resolver.resolve("%Sample.file%"));
    }

    @Test
    public void resolveAll() {
        MCRConfiguration.instance().set("Sample.basedir", "/home/user/base");
        MCRConfiguration.instance().set("Sample.subdir", "%Sample.basedir%/subdir");
        MCRConfiguration.instance().set("Sample.file", "%Sample.subdir%/file.txt");
        Map<String, String> p = MCRConfiguration.instance().getPropertiesMap();
        MCRPropertiesResolver resolver = new MCRPropertiesResolver(p);
        Map<String, String> resolvedProperties = resolver.resolveAll(p);
        assertEquals("/home/user/base/subdir", resolvedProperties.get("Sample.subdir"));
        assertEquals("/home/user/base/subdir/file.txt", resolvedProperties.get("Sample.file"));
    }

    @Test
    public void selfReference() {
        MCRConfiguration.instance().set("a", "%a%,hallo");
        MCRConfiguration.instance().set("b", "hallo,%b%,welt");
        MCRConfiguration.instance().set("c", "%b%,%a%");
        Map<String, String> p = MCRConfiguration.instance().getPropertiesMap();
        MCRPropertiesResolver resolver = new MCRPropertiesResolver(p);
        assertEquals("hallo", resolver.resolve("%a%"));
        assertEquals("hallo,welt", resolver.resolve("%b%"));
        assertEquals("hallo,welt,hallo", resolver.resolve("%c%"));
    }

}
