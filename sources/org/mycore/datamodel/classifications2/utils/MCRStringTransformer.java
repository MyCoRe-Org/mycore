/**
 * 
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.datamodel.classifications2.utils;

import java.util.Collection;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRLabel;

public class MCRStringTransformer {

    public static String getString(MCRCategory category) {
        StringBuilder sb = new StringBuilder(1024);
        printCatgory(category, sb);
        return sb.toString();
    }

    private static void printCatgory(MCRCategory category, StringBuilder sb) {
        for (int i = 0; i < category.getLevel(); i++) {
            sb.append(' ');
        }
        sb.append(category.getId());
        sb.append('[');
        printLabels(category.getLabels().values(), sb);
        sb.append(']');
        sb.append('\n');
        for (MCRCategory child : category.getChildren()) {
            printCatgory(child, sb);
        }
    }

    private static void printLabels(Collection<MCRLabel> labels, StringBuilder sb) {
        for (MCRLabel label : labels) {
            sb.append(label);
            sb.append(',');
        }
        if (labels.size() > 0)
            sb.deleteCharAt(sb.length() - 1);
    }

}
