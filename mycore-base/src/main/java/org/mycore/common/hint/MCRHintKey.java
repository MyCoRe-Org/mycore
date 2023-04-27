/*
 *
 * $Revision$ $Date$
 *
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 *
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.common.hint;

import java.util.Objects;
import java.util.function.Function;

/**
 * A key for {@link MCRHints}.
 *
 * @param <T> The type for values used in conjunction with this key.
 */
public final class MCRHintKey<T> implements Comparable<MCRHintKey<?>>{

    private final String name;

    private final Function<T, String> formatter;

    public MCRHintKey(Class<T> hintClass, String name, Function<T, String> formatter) {
        Objects.requireNonNull(hintClass);
        this.name = Objects.requireNonNull(name);
        this.formatter = Objects.requireNonNull(formatter);
    }

    public String format(T value) {
        if (value == null) {
            return "null";
        }
        String formattedValue = formatter.apply(value);
        if (formattedValue == null) {
            return value.getClass().getName();
        }
        return formattedValue;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(MCRHintKey<?> other) {
        return name.compareTo(other.name);
    }

}
