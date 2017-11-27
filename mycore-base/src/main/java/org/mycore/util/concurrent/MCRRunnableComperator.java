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

package org.mycore.util.concurrent;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

/**
 * A {@link CompletableFuture} encapsulates a {@link Supplier} with an <code>AsyncSupply</code>.
 * This make a {@link ThreadPoolExecutor} with a {@link PriorityBlockingQueue} useless. To
 * regain the {@link Comparable} feature we have to unwrap the <code>AsyncSupply</code> and
 * compare the original {@link Runnable}.
 * 
 * <a href="http://stackoverflow.com/questions/34866757/how-do-i-use-completablefuture-supplyasync-together-with-priorityblockingqueue">stackoverflow</a>
 * 
 * Note: this comparator imposes orderings that are inconsistent with equals.
 * 
 * @author Matthias Eichner
 */
public class MCRRunnableComperator implements Comparator<Runnable> {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public int compare(Runnable o1, Runnable o2) {
        if (isComparable(o1, o2)) {
            return ((Comparable) o1).compareTo(o2);
        }
        // T might be AsyncSupply, UniApply, etc., but we want to
        // compare our original Runnables.
        Comparable unwrap1 = unwrap(o1);
        Comparable unwrap2 = unwrap(o2);

        if (unwrap1 != null) {
            return unwrap1.compareTo(unwrap2);
        }
        return -1;
    }

    private boolean isComparable(Runnable o1, Runnable o2) {
        return o1 instanceof Comparable && o2 instanceof Comparable;
    }

    @SuppressWarnings({ "rawtypes" })
    private Comparable unwrap(Runnable r) {
        try {
            Field field = r.getClass().getDeclaredField("fn");
            field.setAccessible(true);
            // NB: For performance-intensive contexts, you may want to
            // cache these in a ConcurrentHashMap<Class<?>, Field>.
            Object object = field.get(r);
            if (object instanceof Comparable) {
                return (Comparable) object;
            }
            return null;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }

}
