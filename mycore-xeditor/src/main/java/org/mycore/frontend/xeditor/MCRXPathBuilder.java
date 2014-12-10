/*
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

package org.mycore.frontend.xeditor;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Parent;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXPathBuilder {

    public static String buildXPath(Object object) {
        if (object instanceof Element)
            return buildXPath((Element) object);
        else if (object instanceof Attribute)
            return buildXPath((Attribute) object);
        else
            return "";
    }

    public static String buildXPath(Attribute attribute) {
        String parentXPath = buildXPath(attribute.getParent());
        if (!parentXPath.isEmpty())
            parentXPath += "/";
        return parentXPath + "@" + attribute.getQualifiedName();
    }

    public static String buildXPath(Element element) {
        if (element == null)
            return "";

        String parentXPath = buildXPath(element.getParent());
        if ((!parentXPath.isEmpty()) || (element.getParent() instanceof Document))
            parentXPath += "/";
        return parentXPath + buildChildPath(element);
    }

    public static String buildChildPath(Element element) {
        return getNamespacePrefix(element) + element.getName() + buildPositionPredicate(element);
    }

    public static String getNamespacePrefix(Element element) {
        Namespace nsElement = element.getNamespace();
        for (Namespace ns : MCRUsedNamespaces.getNamespaces())
            if (ns.equals(nsElement))
                return ns.getPrefix() + ":";

        String prefix = nsElement.getPrefix();
        if ((prefix != null) && !prefix.isEmpty())
            return prefix + ":";
        else
            return "";
    }

    private static String buildPositionPredicate(Element element) {
        Parent parent = element.getParent();
        if ((parent instanceof Document) || (parent == null))
            return "";

        Element parentElement = (Element) parent;
        int pos = parentElement.getChildren(element.getName(), element.getNamespace()).indexOf(element);
        return (pos == 0 ? "" : "[" + ++pos + "]");
    }
}
