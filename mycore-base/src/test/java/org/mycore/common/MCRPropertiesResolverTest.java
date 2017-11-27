/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

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
