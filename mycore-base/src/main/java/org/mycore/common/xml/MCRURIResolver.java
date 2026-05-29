/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.common.xml;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom2.Element;
import org.mycore.common.config.annotation.MCRFactory;
import org.mycore.common.xsl.MCRXSLResourceHelper;
import org.mycore.common.xsl.uriresolver.MCRURIResolverHelper;
import org.mycore.common.xsl.uriresolver.MCRURIResolverResponse;

/**
 * Reads XML documents from various URI types. This resolver is used to read DTDs, XML Schema files, XSL document()
 * usages, xsl:include usages and MyCoRe Editor include declarations. DTDs and Schema files are read from the CLASSPATH
 * of the application when XML is parsed. XML document() calls and xsl:include calls within XSL stylesheets can be read
 * from URIs of type resource, webapp, file, session, query or mcrobject. MyCoRe editor include declarations can read
 * XML files from resource, webapp, file, session, http or https, query, or mcrobject URIs.
 *
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 *
 * @deprecated Use {@link org.mycore.common.xsl.uriresolver.MCRURIResolver} instead.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("removal")
public final class MCRURIResolver implements URIResolver {

    public MCRURIResolver() {
        reinitialize();
    }

    public void reinitialize() {
        org.mycore.common.xsl.uriresolver.MCRURIResolver.obtainInstance().reinitialize();
    }

    @MCRFactory
    public static MCRURIResolver obtainInstance() {
        return LazyInstanceHolder.SHARED_INSTANCE;
    }

    public static MCRURIResolver createInstance() {
        return new MCRURIResolver();
    }

    public static Map<String, String> getParameterMap(String key) {
        return MCRURIResolverHelper.parseQueryParameters(key);
    }

    /**
     * creates the default boolean response of a MyCoRe URIResolver.
     * This is an element with text body: &lt;boolean&gt;true|false&lt;boolean&gt;
     * @param value the boolean value that should be returned
     * @return a JDOMSource
     */
    public static Source createBooleanResponse(boolean value) {
        return MCRURIResolverResponse.ofBoolean(value);
    }

    /**
     * creates the default String response of a MyCoRe URIResolver.
     * This is an element with text body: &lt;string&gt;texte&lt;string&gt;
     * @param text the String text that should be returned
     * @return a JDOMSource
     */
    public static Source createStringResponse(String text) {
        return MCRURIResolverResponse.ofString(text);
    }

    static URI resolveURI(String href, String base) {
        return Optional.ofNullable(base)
            .map(URI::create)
            .map(u -> u.resolve(href))
            .orElse(URI.create(href));
    }

    /**
     * Tries to calculate the resource uri to the directory of the stylesheet that includes the given file.
     *
     * @param base the base uri of the stylesheet that includes the given file
     * @return the resource uri to the directory of the stylesheet that includes the given file.
     */
    static String getParentDirectoryResourceURI(String base) {
        return MCRXSLResourceHelper.getXSLDirectory(base);
    }

    /**
     * URI Resolver that resolves XSL document() or xsl:include calls.
     *
     * @see URIResolver
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        return org.mycore.common.xsl.uriresolver.MCRURIResolver.obtainInstance().resolve(href, base);
    }

    /**
     * Reads XML from URIs of various type.
     *
     * @param uri
     *            the URI where to read the XML from
     * @return the root element of the XML document
     */
    public Element resolve(String uri) {
        return org.mycore.common.xsl.uriresolver.MCRURIResolver.obtainInstance().resolve(uri);
    }

    /**
     * Returns the protocol or scheme for the given URI.
     *
     * @param uri
     *            the URI to parse
     * @param base
     *            if uri is relative, resolve scheme from base parameter
     * @return the protocol/scheme part before the ":"
     */
    public String getScheme(String uri, String base) {
        return org.mycore.common.xsl.uriresolver.MCRURIResolver.obtainInstance().getScheme(uri, base);
    }

    /**
     * provides a URI -- Resolver Mapping One can implement this interface to provide additional URI schemes this
     * MCRURIResolver should handle, too. To add your mapping you have to set the
     * <code>MCR.URIResolver.ExternalResolver.Class</code> property to the implementing class.
     *
     * @author Thomas Scheffler
     */
    public interface MCRResolverProvider {
        /**
         * provides a Map of URIResolver mappings. Key is the scheme, e.g. <code>http</code>, where value is an
         * implementation of {@link URIResolver}.
         *
         * @see URIResolver
         * @return a Map of URIResolver mappings
         */
        Map<String, URIResolver> getURIResolverMapping();
    }

    public interface MCRXslIncludeHrefs {
        List<String> getHrefs();
    }

    private static final class LazyInstanceHolder {
        public static final MCRURIResolver SHARED_INSTANCE = createInstance();
    }

}
