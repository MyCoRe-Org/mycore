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

package org.mycore.common.xsl;

import java.io.IOException;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;

import org.mycore.common.MCRCache;
import org.mycore.common.config.MCRConfiguration;

/**
 * Returns a Transformer for a given XSL source, providing caching of
 * already compiled XSL stylesheets. The cache size can be set via
 * MCR.LayoutService.XSLCacheSize (default is 200).
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXSLTransformerFactory {

    /** A cache of already compiled stylesheets */
    private static MCRCache<String, Templates> cache;

    private static long checkPeriod;

    static {
        int cacheSize = MCRConfiguration.instance().getInt("MCR.LayoutService.XSLCacheSize", 200);
        checkPeriod = MCRConfiguration.instance().getLong("MCR.LayoutService.LastModifiedCheckPeriod", 10000);
        cache = new MCRCache<>(cacheSize, MCRXSLTransformerFactory.class.getName());
    }

    /** Returns the compiled XSL templates cached for the given source, if it is up-to-date. 
     * @throws IOException */
    private static Templates getCachedTemplates(MCRTemplatesSource source) throws IOException {
        String key = source.getKey();
        return cache.getIfUpToDate(key, source.getModifiedHandle(checkPeriod));
    }

    /** Compiles the given XSL source, and caches the result */
    private static Templates compileTemplates(MCRTemplatesSource source) {
        Templates templates = MCRTemplatesCompiler.compileTemplates(source);
        cache.put(source.getKey(), templates);
        return templates;
    }

    /** Returns a transformer for the given XSL source
     */
    public static Transformer getTransformer(MCRTemplatesSource source)
        throws IOException, TransformerConfigurationException {
        Templates templates = getCachedTemplates(source);
        if (templates == null) {
            templates = compileTemplates(source);
        }
        return MCRTemplatesCompiler.getTransformer(templates);
    }
}
