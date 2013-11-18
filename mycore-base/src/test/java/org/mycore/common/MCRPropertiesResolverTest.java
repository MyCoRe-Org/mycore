package org.mycore.common;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;

public class MCRPropertiesResolverTest {

    @Test
    public void resolve() {
        MCRConfiguration.instance().set("Sample.basedir", "/home/user/base");
        MCRConfiguration.instance().set("Sample.subdir", "%Sample.basedir%/subdir");
        MCRConfiguration.instance().set("Sample.file", "%Sample.subdir%/file.txt");
        MCRPropertiesResolver resolver = new MCRPropertiesResolver(MCRConfiguration.instance().getProperties());
        assertEquals("/home/user/base", resolver.resolve("%Sample.basedir%"));
        assertEquals("/home/user/base/subdir", resolver.resolve("%Sample.subdir%"));
        assertEquals("/home/user/base/subdir/file.txt", resolver.resolve("%Sample.file%"));
    }

    @Test
    public void resolveAll() {
        MCRConfiguration.instance().set("Sample.basedir", "/home/user/base");
        MCRConfiguration.instance().set("Sample.subdir", "%Sample.basedir%/subdir");
        MCRConfiguration.instance().set("Sample.file", "%Sample.subdir%/file.txt");
        Properties p = MCRConfiguration.instance().getProperties();
        MCRPropertiesResolver resolver = new MCRPropertiesResolver(p);
        Properties resolvedProperties = resolver.resolveAll(p);
        assertEquals("/home/user/base/subdir", resolvedProperties.get("Sample.subdir"));
        assertEquals("/home/user/base/subdir/file.txt", resolvedProperties.get("Sample.file"));
    }

}
