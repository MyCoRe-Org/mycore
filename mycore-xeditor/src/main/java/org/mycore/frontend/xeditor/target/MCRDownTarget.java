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

package org.mycore.frontend.xeditor.target;

import org.jdom2.Element;
import org.mycore.frontend.xeditor.MCRBinding;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRDownTarget extends MCRControlTarget {

    @Override
    protected void handleControl(MCRBinding baseBinding, String repeatXPath, String pos) throws Exception {
        String xPath = repeatXPath + "[" + pos + "]";
        MCRBinding binding = new MCRBinding(xPath, baseBinding);
        Element element = (Element) (binding.getBoundNode());
        Element parent = element.getParentElement();
        int posInParent = parent.indexOf(element);
        element.detach();
        parent.addContent(posInParent + 1, element);
    }
}
