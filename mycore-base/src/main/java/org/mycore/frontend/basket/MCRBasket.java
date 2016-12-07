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

package org.mycore.frontend.basket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Collectors;

import org.jdom2.Element;

/**
 * Implements a basket of entries.
 * The basket has a type attribute that allows to
 * distinguish multiple baskets within the same session.
 * The basket implements the List and Set interfaces,
 * it behaves like an ordered Set of entries. 
 * Entries already contained in the basket can not be 
 * re-added, each entry can be contained only once in the basket. 
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRBasket implements List<MCRBasketEntry>, Set<MCRBasketEntry> {

    /** The internal list of basket entries */
    private List<MCRBasketEntry> list = new ArrayList<MCRBasketEntry>();

    /** The type of basket */
    private String type;

    /** The ID of the derivate that holds the persistent data of this basket */
    private String derivateID;

    /**
     * Creates a new basket of the given type.
     * 
     * @param type the type of basket, an attribute that can be used by the application to distinguish multiple basket within the same session.
     */
    public MCRBasket(String type) {
        this.type = type;
    }

    /**
     * Returns the type of basket.
     */
    public String getType() {
        return type;
    }

    /** 
     * Returns the ID of the derivate that holds the persistent data of this basket, or null
     */
    public String getDerivateID() {
        return derivateID;
    }

    /**
     * Sets the ID of the derivate that holds the persistent data of this basket
     */
    public void setDerivateID(String derivateID) {
        this.derivateID = derivateID;
    }

    @Override
    public void add(int index, MCRBasketEntry entry) {
        if (!contains(entry))
            list.add(index, entry);
    }

    @Override
    public boolean add(MCRBasketEntry entry) {
        return !contains(entry) && list.add(entry);
    }

    @Override
    public boolean addAll(Collection<? extends MCRBasketEntry> collection) {
        boolean changed = false;
        for (MCRBasketEntry entry : collection)
            if (!contains(entry)) {
                changed = true;
                add(entry);
            }
        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<? extends MCRBasketEntry> collection) {
        return list.addAll(index, collection.stream().filter(entry -> !contains(entry)).collect(Collectors.toList()));
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    @Override
    public MCRBasketEntry get(int index) {
        return list.get(index);
    }

    /**
     * Returns the basket entry with the given ID, or null.
     *
     * @param id the ID of the basket entry.
     */
    public MCRBasketEntry get(String id) {
        return this.stream().filter(entry -> id.equals(entry.getID())).findFirst().orElse(null);
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public Iterator<MCRBasketEntry> iterator() {
        return list.iterator();
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<MCRBasketEntry> listIterator() {
        return list.listIterator();
    }

    @Override
    public ListIterator<MCRBasketEntry> listIterator(int index) {
        return list.listIterator(index);
    }

    @Override
    public MCRBasketEntry remove(int index) {
        return list.remove(index);
    }

    @Override
    public boolean remove(Object o) {
        return list.remove(o);
    }

    /**
     * Removes the entry with the given ID from the basket.
     * 
     * @return true, if the basket entry was removed successfully.
     */
    public boolean removeEntry(String id) {
        MCRBasketEntry entry = get(id);
        if (entry == null)
            return false;
        return remove(entry);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return list.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return list.retainAll(c);
    }

    @Override
    public MCRBasketEntry set(int index, MCRBasketEntry entry) {
        return (contains(entry) ? null : list.set(index, entry));
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public List<MCRBasketEntry> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }

    /**
     * Moves the basket entry one position up in the
     * list of basket entries.
     */
    public void up(MCRBasketEntry entry) {
        move(entry, -1);
    }

    /**
     * Moves the basket entry one position down in the
     * list of basket entries.
     */
    public void down(MCRBasketEntry entry) {
        move(entry, 1);
    }

    /**
     * Moves a basket entry up or down in the
     * list of basket entries.
     * 
     * @param change the number of index positions to move the entry.
     */
    public void move(MCRBasketEntry entry, int change) {
        int posOld = indexOf(entry);
        int posNew = posOld + change;
        if ((posNew < 0) || (posNew > list.size() - 1))
            return;

        remove(posOld);
        add(posNew, entry);
    }

    /**
     * For all basket entries, if their XML content is not resolved yet, 
     * resolves the XML from the given URI in the entry.
     */
    public void resolveContent() {
        for (MCRBasketEntry entry : list) {
            Element content = entry.getContent();
            if (content == null)
                entry.resolveContent();
        }
    }

    @Override
    public Spliterator<MCRBasketEntry> spliterator() {
        return list.spliterator();
    }
}
