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

package org.mycore.common;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.jdom2.Namespace;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.config.MCRConfiguration2;

/**
 * This class replaces the deprecated MCRDefaults interface and provides some
 * final static fields of common use.
 *
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @author Stefan Freitag (sasf)
 * @author Frank Lützenkirchen
 */
public final class MCRConstants {

    /** MCR.Metadata.DefaultLang */
    public static final String DEFAULT_LANG = "de";

    /** The default encoding */
    public static final String DEFAULT_ENCODING = "UTF-8";

    public static final Namespace XML_NAMESPACE = Namespace.getNamespace("xml", "http://www.w3.org/XML/1998/namespace");

    public static final Namespace XLINK_NAMESPACE = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

    /** the MARC 21 namespace */
    public static final Namespace MARC21_NAMESPACE = Namespace.getNamespace("marc21", "http://www.loc.gov/MARC21/slim");

    /** MARC 21 namespace schema location */
    public static final String MARC21_SCHEMA_LOCATION = "http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd";

    public static final XPathFactory XPATH_FACTORY = XPathFactory.instance();

    /** The URL of the XSI */
    private static final String XSI_URL = "http://www.w3.org/2001/XMLSchema-instance";

    public static final Namespace XSI_NAMESPACE = Namespace.getNamespace("xsi", XSI_URL);

    /** The URL of the XSL */
    private static final String XSL_URL = "http://www.w3.org/1999/XSL/Transform";

    public static final Namespace XSL_NAMESPACE = Namespace.getNamespace("xsl", XSL_URL);

    /** The URL of the METS */
    private static final String METS_URL = "http://www.loc.gov/METS/";

    public static final Namespace METS_NAMESPACE = Namespace.getNamespace("mets", METS_URL);

    /** The URL of the DV */
    private static final String DV_URL = "http://dfg-viewer.de/";

    public static final Namespace DV_NAMESPACE = Namespace.getNamespace("dv", DV_URL);

    /** The URL of the LIDO */
    private static final String LIDO_URL = "http://www.lido-schema.org";

    public static final Namespace LIDO_NAMESPACE = Namespace.getNamespace("lido", LIDO_URL);

    /** The URL of the MODS */
    private static final String MODS_URL = "http://www.loc.gov/mods/v3";

    public static final Namespace MODS_NAMESPACE = Namespace.getNamespace("mods", MODS_URL);

    public static final Namespace ZS_NAMESPACE = Namespace.getNamespace("zs", "http://www.loc.gov/zing/srw/");

    public static final Namespace ZR_NAMESPACE = Namespace.getNamespace("zr", "http://explain.z3950.org/dtd/2.0/");

    public static final Namespace SRW_NAMESPACE = Namespace.getNamespace("srw", "http://www.loc.gov/zing/srw/");

    public static final Namespace INFO_SRW_NAMESPACE = Namespace.getNamespace("info", "info:srw/schema/5/picaXML-v1.0");

    public static final Namespace PIDEF_NAMESPACE = Namespace.getNamespace("pidef", "http://nbn-resolving.org/pidef");

    public static final Namespace CROSSREF_NAMESPACE = Namespace.getNamespace("cr",
        "http://www.crossref.org/schema/4.4.1");

    public static final Namespace DIAG_NAMESPACE = Namespace.getNamespace("diag",
        "http://www.loc.gov/zing/srw/diagnostic");

    public static final Namespace EPICURLITE_NAMESPACE = Namespace.getNamespace("epicurlite",
        "http://nbn-resolving.org/epicurlite");

    public static final Namespace ALTO_NAMESPACE = Namespace
        .getNamespace("alto", "http://www.loc.gov/standards/alto/ns-v2#");

    public static final Namespace SKOS_NAMESPACE = Namespace
        .getNamespace("skos", "http://www.w3.org/2004/02/skos/core#");

    public static final Namespace RDF_NAMESPACE = Namespace
        .getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

    /** The URL of the MCR */
    private static final String MCR_URL = "http://www.mycore.org/";

    public static final Namespace MCR_NAMESPACE = Namespace.getNamespace("mcr", MCR_URL);

    private static final Map<String, Namespace> NAMESPACES_BY_PREFIX;

    static {
        NAMESPACES_BY_PREFIX = new ConcurrentHashMap<>();

        Field[] fields = MCRConstants.class.getFields();
        for (Field f : fields) {
            if (f.getType() == Namespace.class) {
                try {
                    Namespace namespace = (Namespace) f.get(null);
                    registerNamespace(namespace);
                } catch (Exception e) {
                    LogManager.getLogger(MCRConstants.class).error(
                        "Error while initialising Namespace list and HashMap",
                        e);
                }
            }
        }
        MCRConfiguration2.getSubPropertiesMap("MCR.Namespace.")
            .forEach(MCRConstants::registerNamespace);
    }

    private static void registerNamespace(String prefix, String uri) {
        registerNamespace(Namespace.getNamespace(prefix, uri));
    }

    /**
     * Adds and registers a standard namespace with prefix.
     * Note that a default namespace without prefix will be ignored here!
     */
    public static void registerNamespace(Namespace namespace) {
        String prefix = namespace.getPrefix();

        if ((prefix != null) && !prefix.isEmpty()) {
            NAMESPACES_BY_PREFIX.put(prefix, namespace);
        }
    }

    /**
     * Returns a list of standard namespaces used in MyCoRe. Additional
     * namespaces can be configured using properties like
     * MCR.Namespace.&lt;prefix&gt;=&lt;uri&gt;
     */
    public static Collection<Namespace> getStandardNamespaces() {
        return NAMESPACES_BY_PREFIX.values();
    }

    /**
     * Returns the namespace with the given standard prefix. Additional
     * namespaces can be configured using properties like
     * MCR.Namespace.&lt;prefix&gt;=&lt;uri&gt;
     */
    public static Namespace getStandardNamespace(String prefix) {
        return NAMESPACES_BY_PREFIX.get(prefix);
    }

}
