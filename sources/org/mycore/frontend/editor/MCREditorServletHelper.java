/*
 * $RCSfile$
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

package org.mycore.frontend.editor;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * This class helps Servlets to construct valid MCREditor templates which can than be uses as default input for certain given fields.
 * 
 * @author Thomas Scheffler (yagee)
 *
 * @version $Revision$ $Date$
 */
public final class MCREditorServletHelper {
    
    private final static String XML_PREFIX="_xml_";
    private final static Logger LOGGER=Logger.getLogger(MCREditorServletHelper.class);
    
    /**
     * builds a JDOM Element hierarchie according to parameters submitted with
     * the request.
     * 
     * given a root element name "mycoreobject" a request parameter<br/>
     * 
     * <pre>
     *    _xml_structure%2Fparents%2Fparent=XYZ
     * </pre>
     * 
     * would result in the following jdom element hierarchy of the root element:
     * 
     * <pre>
     *  &lt;mycoreobject&gt;
     *   &lt;structure&gt;
     *    &lt;parents&gt;
     *     &lt;parent&gt;XYZ&lt;/parent&gt;
     *    &lt;/parents&gt;
     *   &lt;/structure&gt;
     *  &lt;/mycoreobject&gt;
     * </pre>
     * 
     * a proper use for this method can be look like this:
     * 
     * <code><pre>
     * Element root = new Element(&quot;mycoreobject&quot;);
     * if (MCREditorServletHelper.buildXMLTemplate(job.getRequest(), root)) {
     *     //there _xml_* parameters and root was changed
     *     MCRSessionMgr.getCurrentSession().put(&quot;wnewobj&quot;, root);
     *     params.put(&quot;XSL.editor.source.url&quot;, &quot;session:wnewobj&quot;);
     * } else {
     *     //start with an empty editor session
     *     params.put(&quot;XSL.editor.source.new&quot;, &quot;true&quot;);
     * }
     * </pre></code>
     * 
     * @param request
     *            a HttpServletRequest that (maybe) contains parameters to build
     *            a xml structure
     * @param root
     *            the root JDOM element under which new elements and attributes
     *            are being inserted
     * @return true, if the root element was changed (attributes or elements
     *         where added)<br/> else: false
     */
    public static boolean buildXMLTemplate(HttpServletRequest request, Element root){
        Enumeration e = request.getParameterNames();
        boolean changedElement=false;

        while (e.hasMoreElements()) {
            String name = (String) (e.nextElement());
            String value = request.getParameter(name);

            if (!name.startsWith(XML_PREFIX)) {
                continue;
            }
            constructElement(root,name.substring(XML_PREFIX.length()),value);
            changedElement=true;
        }
        return changedElement;
    }
    
    private static void constructElement(Element current, String xpath, String value) {
        int i = xpath.indexOf('/');
        LOGGER.debug("Processing xpath: "+xpath);
        String subname = xpath;
        if (i > 0) {
            //construct new element name and xpath value
            subname = xpath.substring(0, i);
            xpath = xpath.substring(i + 1);
        }
        i = xpath.indexOf('/');
        if (subname.startsWith("@")) {
            if (i > 0) {
                subname = subname.substring(0, i);//attribute should be the last
            }
            subname=subname.substring(1);//remove @
            LOGGER.debug("Setting attribute "+subname+"="+value);
            current.setAttribute(subname, value);
            return;
        }
        Element newcurrent = current.getChild(subname);
        if (newcurrent == null) {
            newcurrent = new Element(subname);
            LOGGER.debug("Adding element "+newcurrent.getName()+" to "+current.getName());
            current.addContent(newcurrent);
        }
        if (subname==xpath) {
            //last element of xpath
            LOGGER.debug("Setting text of element "+newcurrent.getName()+" to "+value);
            newcurrent.setText(value);
            return;
        }
        constructElement(newcurrent, xpath, value); //recursive call
    }

}