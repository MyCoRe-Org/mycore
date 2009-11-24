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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.jdom.Namespace;

/**
 * This class replaces the deprecated MCRDefaults interface and provides some
 * final static fields of common use.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @author Stefan Freitag (sasf)
 * @author Frank Lützenkirchen
 * 
 * @version $Revision$ $Date$
 */
public final class MCRConstants {
    /** MyCoRe version */
    public final static String VERSION = "2.0";
    
    /** The URL of the XLink */
    private final static String XLINK_URL = "http://www.w3.org/1999/xlink";
    public final static Namespace XLINK_NAMESPACE = Namespace.getNamespace("xlink", XLINK_URL);

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
    
    /** The URL of the MCR */
    private final static String MCR_URL = "http://www.mycore.org/";
    public final static Namespace MCR_NAMESPACE = Namespace.getNamespace("mcr", MCR_URL);

    private final static List<Namespace> namespaces;
    
    private final static HashMap<String, Namespace> namespacesByPrefix;

    static {
        namespaces = new ArrayList<Namespace>();
        namespaces.add(XLINK_NAMESPACE);
        namespaces.add(XSI_NAMESPACE);
        namespaces.add(XSL_NAMESPACE);
        namespaces.add(METS_NAMESPACE);
        namespaces.add(DV_NAMESPACE);
        namespaces.add(MODS_NAMESPACE);
        namespaces.add(MCR_NAMESPACE);

        namespacesByPrefix = new HashMap<String, Namespace>();
        namespacesByPrefix.put("xlink", XLINK_NAMESPACE);
        namespacesByPrefix.put("xsi", XSI_NAMESPACE);
        namespacesByPrefix.put("xsl", XSL_NAMESPACE);
        namespacesByPrefix.put("mets", METS_NAMESPACE);
        namespacesByPrefix.put("dv", DV_NAMESPACE);
        namespacesByPrefix.put("mods", MODS_NAMESPACE);
        namespacesByPrefix.put("mcr", MCR_NAMESPACE);
        
        Properties p = MCRConfiguration.instance().getProperties("MCR.Namespace");
        for (Iterator it = p.keySet().iterator(); it.hasNext();) {
            String prefix = (String) it.next();
            String uri = p.getProperty(prefix);
            prefix = prefix.substring(prefix.lastIndexOf(".") + 1);
            Namespace ns = Namespace.getNamespace(prefix, uri);
            namespacesByPrefix.put(prefix, ns);
            namespaces.add(ns);
        }
    }

    /**
     * Returns a list of standard namespaces used in MyCoRe.
     * Additional namespaces can be configured using properties like
     * MCR.Namespace.<prefix>=<uri>
     */
    public static List<Namespace> getStandardNamespaces() {
        return namespaces;
    }
    
    /**
     * Returns the namespace with the given standard prefix.
     * Additional namespaces can be configured using properties like
     * MCR.Namespace.<prefix>=<uri>
     */
    public static Namespace getStandardNamespace( String prefix ){ 
        return namespacesByPrefix.get(prefix);
    }
    
    /** The default encoding */
    public final static String DEFAULT_ENCODING = "UTF-8";

    /** The date format for the supported languages * */
    public static DateFormat[] DATE_FORMAT = {
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()), // x-...
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMAN), // de,ch,au
        DateFormat.getDateInstance(DateFormat.SHORT, Locale.UK), // ar,en-UK
        DateFormat.getDateInstance(DateFormat.SHORT, Locale.US), // en-US
    };
}
