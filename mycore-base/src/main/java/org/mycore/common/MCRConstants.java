/*
 * 
 * $Revision$ 
 * $Date$
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

package org.mycore.common;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.jdom2.Namespace;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.config.MCRConfiguration;

/**
 * This class replaces the deprecated MCRDefaults interface and provides some
 * final static fields of common use.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @author Stefan Freitag (sasf)
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date: 2011-06-22 12:50:42 +0200 (Wed, 22 Jun
 *          2011) $
 */
public final class MCRConstants {

    /** MCR.Metadata.DefaultLang */
    public static final String DEFAULT_LANG = "de";

    public final static Namespace XML_NAMESPACE = Namespace.getNamespace("xml", "http://www.w3.org/XML/1998/namespace");

    public final static Namespace XLINK_NAMESPACE = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

    /** the MARC 21 namespace */
    public final static Namespace MARC21_NAMESPACE = Namespace.getNamespace("marc21", "http://www.loc.gov/MARC21/slim");

    /** MARC 21 namespace schema location */
    public final static String MARC21_SCHEMA_LOCATION = "http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd";

    public final static XPathFactory XPATH_FACTORY = XPathFactory.instance();

    /** The URL of the XSI */
    private final static String XSI_URL = "http://www.w3.org/2001/XMLSchema-instance";

    public final static Namespace XSI_NAMESPACE = Namespace.getNamespace("xsi", XSI_URL);

    /** The URL of the XSL */
    private final static String XSL_URL = "http://www.w3.org/1999/XSL/Transform";

    public final static Namespace XSL_NAMESPACE = Namespace.getNamespace("xsl", XSL_URL);

    /** The URL of the METS */
    private final static String METS_URL = "http://www.loc.gov/METS/";

    public final static Namespace METS_NAMESPACE = Namespace.getNamespace("mets", METS_URL);

    /** The URL of the DV */
    private final static String DV_URL = "http://dfg-viewer.de/";

    public final static Namespace DV_NAMESPACE = Namespace.getNamespace("dv", DV_URL);

    /** The URL of the MODS */
    private final static String MODS_URL = "http://www.loc.gov/mods/v3";

    public final static Namespace MODS_NAMESPACE = Namespace.getNamespace("mods", MODS_URL);

    public static final Namespace ZS_NAMESPACE = Namespace.getNamespace("zs", "http://www.loc.gov/zing/srw/");

    public static final Namespace ZR_NAMESPACE = Namespace.getNamespace("zr", "http://explain.z3950.org/dtd/2.0/");

    public static final Namespace SRW_NAMESPACE = Namespace.getNamespace("srw", "http://www.loc.gov/zing/srw/");

    public static final Namespace INFO_SRW_NAMESPACE = Namespace.getNamespace("info", "info:srw/schema/5/picaXML-v1.0");

    public static final Namespace PIDEF_NAMESPACE = Namespace.getNamespace("pidef", "http://nbn-resolving.org/pidef");

    public static final Namespace DIAG_NAMESPACE = Namespace.getNamespace("diag",
        "http://www.loc.gov/zing/srw/diagnostic");

    public static final Namespace EPICURLITE_NAMESPACE = Namespace.getNamespace("epicurlite",
        "http://nbn-resolving.org/epicurlite");

    /** The URL of the MCR */
    private final static String MCR_URL = "http://www.mycore.org/";

    public final static Namespace MCR_NAMESPACE = Namespace.getNamespace("mcr", MCR_URL);

    private final static HashMap<String, Namespace> namespacesByPrefix;

    static {
        namespacesByPrefix = new HashMap<String, Namespace>();

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

        Map<String, String> p = MCRConfiguration.instance().getPropertiesMap("MCR.Namespace");
        for (String prefix : p.keySet()) {
            String uri = p.get(prefix);
            prefix = prefix.substring(prefix.lastIndexOf(".") + 1);
            Namespace ns = Namespace.getNamespace(prefix, uri);
            registerNamespace(ns);
        }
    }

    /**
     * Adds and registers a standard namespace with prefix.
     * Note that a default namespace without prefix will be ignored here!
     */
    public static void registerNamespace(Namespace namespace) {
        String prefix = namespace.getPrefix();

        if ((prefix != null) && !prefix.isEmpty()) {
            namespacesByPrefix.put(prefix, namespace);
        }
    }

    /**
     * Returns a list of standard namespaces used in MyCoRe. Additional
     * namespaces can be configured using properties like
     * MCR.Namespace.&lt;prefix&gt;=&lt;uri&gt;
     */
    public static Collection<Namespace> getStandardNamespaces() {
        return namespacesByPrefix.values();
    }

    /**
     * Returns the namespace with the given standard prefix. Additional
     * namespaces can be configured using properties like
     * MCR.Namespace.&lt;prefix&gt;=&lt;uri&gt;
     */
    public static Namespace getStandardNamespace(String prefix) {
        return namespacesByPrefix.get(prefix);
    }

    /** The default encoding */
    public final static String DEFAULT_ENCODING = "UTF-8";

}
