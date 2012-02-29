/*
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common.xsl;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;

import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;

/**
 * Returns a Transformer for a given XSL source, providing caching of
 * already compiled XSL stylesheets. The cache size can be set via
 * MCR.LayoutService.XSLCacheSize (default is 200).
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXSLTransformerFactory {

    /** A cache of already compiled stylesheets */
    private static MCRCache cache;

    static {
        int cacheSize = MCRConfiguration.instance().getInt("MCR.LayoutService.XSLCacheSize", 200);
        cache = new MCRCache(cacheSize, MCRXSLTransformerFactory.class.getName());
    }

    /** Returns the compiled XSL templates cached for the given source, if it is up-to-date. */
    private static Templates getCachedTemplates(MCRTemplatesSource source) {
        long lastModified = source.getLastModified();
        String key = source.getKey();
        return (Templates) (cache.getIfUpToDate(key, lastModified));
    }

    /** Compiles the given XSL source, and caches the result */
    private static Templates compileTemplates(MCRTemplatesSource source) {
        Templates templates = MCRTemplatesCompiler.compileTemplates(source);
        cache.put(source.getKey(), templates);
        return templates;
    }

    /** Returns a transformer for the given XSL source */
    public static Transformer getTransformer(MCRTemplatesSource source) {
        Templates templates = getCachedTemplates(source);
        if (templates == null) {
            templates = compileTemplates(source);
        }
        return MCRTemplatesCompiler.getTransformer(templates);
    }
}
