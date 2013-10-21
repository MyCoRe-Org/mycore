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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jdom2.Namespace;
import org.jdom2.xpath.XPathFactory;

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
    /** MyCoRe version
     *  This sticks to "2.0"
     * @deprecated use {@link MCRCoreVersion#getVersion()} to get mycore version 
     */
    public final static String VERSION = "2.0";

    /** MCR.Metadata.DefaultLang */
    public static final String DEFAULT_LANG = "de";

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

    /** The URL of the MCR */
    private final static String MCR_URL = "http://www.mycore.org/";

    public final static Namespace MCR_NAMESPACE = Namespace.getNamespace("mcr", MCR_URL);

    @Deprecated
    public final static String SUPER_USER_ID = MCRSystemUserInformation.getSuperUserInstance().getUserID();

    private final static List<Namespace> namespaces;

    private final static HashMap<String, Namespace> namespacesByPrefix;

    static {
        namespacesByPrefix = new HashMap<String, Namespace>();
        namespaces = new ArrayList<Namespace>();

        Field[] fields = MCRConstants.class.getFields();
        for (Field f : fields) {
            if (f.getType() == Namespace.class) {
                try {
                    Namespace namespace = (Namespace) f.get(null);
                    namespaces.add(namespace);
                    namespacesByPrefix.put(namespace.getPrefix(), namespace);
                } catch (Exception e) {
                    Logger.getLogger(MCRConstants.class).error("Error while initialising Namespace list and HashMap", e);
                }
            }
        }

        Properties p = MCRConfiguration.instance().getProperties("MCR.Namespace");
        for (Object element : p.keySet()) {
            String prefix = (String) element;
            String uri = p.getProperty(prefix);
            prefix = prefix.substring(prefix.lastIndexOf(".") + 1);
            Namespace ns = Namespace.getNamespace(prefix, uri);
            namespacesByPrefix.put(prefix, ns);
            namespaces.add(ns);
        }
    }

    /**
     * Returns a list of standard namespaces used in MyCoRe. Additional
     * namespaces can be configured using properties like
     * MCR.Namespace.<prefix>=<uri>
     */
    public static List<Namespace> getStandardNamespaces() {
        return namespaces;
    }

    /**
     * Returns the namespace with the given standard prefix. Additional
     * namespaces can be configured using properties like
     * MCR.Namespace.<prefix>=<uri>
     */
    public static Namespace getStandardNamespace(String prefix) {
        return namespacesByPrefix.get(prefix);
    }

    /** The default encoding */
    public final static String DEFAULT_ENCODING = "UTF-8";

    /** The date format for the supported languages * */
    public static DateFormat[] DATE_FORMAT = { DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()), // x-...
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMAN), // de,ch,au
            DateFormat.getDateInstance(DateFormat.SHORT, Locale.UK), // ar,en-UK
            DateFormat.getDateInstance(DateFormat.SHORT, Locale.US), // en-US
    };
}
