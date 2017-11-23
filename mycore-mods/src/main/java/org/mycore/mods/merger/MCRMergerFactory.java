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

package org.mycore.mods.merger;

import org.jdom2.Element;
import org.mycore.common.config.MCRConfiguration;

/**
 * Builds appropriate MCRMerger instances for a given MODS XML element.
 * Since MODS elements need to be compared and merged differently, the factory returns
 * different merger implementations for different element types.
 *
 * MCR.MODS.Merger.default=[Default class to merge MODS, typically MCRMerger]
 * MCR.MODS.Merger.[elementName]=[Specific implementation by element name]
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRMergerFactory {

    /**
     * Returns an MCRMerger instance usable for merging MODS elements with the given name
     */
    private static MCRMerger getEntryInstance(String name) {
        String prefix = "MCR.MODS.Merger.";
        String defaultClass = MCRConfiguration.instance().getString(prefix + "default");
        return (MCRMerger) (MCRConfiguration.instance().getInstanceOf(prefix + name, defaultClass));
    }

    /**
     * Returns an MCRMerger instance usable to merge the given MODS element.
     */
    public static MCRMerger buildFrom(Element element) {
        String name = element.getName();
        MCRMerger entry = getEntryInstance(name);
        entry.setElement(element);
        return entry;
    }
}
