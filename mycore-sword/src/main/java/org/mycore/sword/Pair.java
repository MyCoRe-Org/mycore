/*
 * 
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

package org.mycore.sword;

import java.io.Serializable;

/**
 * A <code>Pair</code> is a data model that contains a two objects which are
 * logically bound together, for example username and password. There is no
 * password without a user!
 * 
 * @author Nils Verheyen
 * 
 * @param <A>
 *            first object inside this data pair
 * @param <B>
 *            second object inside this data pair
 */
public final class Pair<A, B> implements Serializable {

    private static final long serialVersionUID = 1L;

    public final A first;

    public final B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Utility method for creating a pair of data. It is only provided to
     * provide better readability.
     * 
     * @param <A>
     *            type of first object
     * @param <B>
     *            type of second object
     * @param first
     *            first obejct
     * @param second
     *            second object
     * @return a new data pair with given objects
     */
    public static <A, B> Pair<A, B> of(A first, B second) {
        return new Pair<A, B>(first, second);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((first == null) ? 0 : first.hashCode());
        result = prime * result + ((second == null) ? 0 : second.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Pair))
            return false;
        Pair<?, ?> other = (Pair<?, ?>) obj;
        if (first == null) {
            if (other.first != null)
                return false;
        } else if (!first.equals(other.first))
            return false;
        if (second == null) {
            if (other.second != null)
                return false;
        } else if (!second.equals(other.second))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Pair [first=" + first + ", second=" + second + "]";
    }

}
