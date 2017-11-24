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
        printLabels(category.getLabels(), sb);
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
        if (labels.size() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
    }

}
