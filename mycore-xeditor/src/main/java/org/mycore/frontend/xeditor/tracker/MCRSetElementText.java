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

package org.mycore.frontend.xeditor.tracker;

import java.util.Iterator;

import org.jdom2.Attribute;
import org.jdom2.Element;

public class MCRSetElementText implements MCRChange {

    public static MCRChangeData setText(Element element, String text) {
        Element clone = element.clone();

        for (Iterator<Attribute> attributes = clone.getAttributes().iterator(); attributes.hasNext();) {
            attributes.next();
            attributes.remove();
        }

        MCRChangeData data = new MCRChangeData("set-text", clone, 0, element);
        element.setText(text);
        return data;
    }

    public void undo(MCRChangeData data) {
        data.getContext().setContent(data.getElement().cloneContent());
    }
}
