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

package org.mycore.common.xsl.extensions;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class Counter {

    private int value;

    public Counter() {
        this.value = 0;
    }

    public static int next(Counter ctr) {
        return ++ctr.value;
    }

    public static int reset(Counter ctr) {
        return set(ctr, 0);
    }

    public static int set(Counter ctr, int newValue) {
        int oldValue = ctr.value;
        ctr.value = newValue;
        return oldValue;
    }

    public static int get(Counter ctr) {
        return ctr.value;
    }

}
