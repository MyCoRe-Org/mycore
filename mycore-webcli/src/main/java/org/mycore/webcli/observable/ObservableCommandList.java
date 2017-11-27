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

package org.mycore.webcli.observable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Observable;
import java.util.concurrent.CopyOnWriteArrayList;

import org.mycore.common.MCRJSONUtils;

/**
 * @author Michel Buechner (mcrmibue)
 * 
 */
public class ObservableCommandList extends Observable {
    CopyOnWriteArrayList<String> arrayList;

    public ObservableCommandList() {
        this.arrayList = new CopyOnWriteArrayList<>();
    }

    public void add(String text) {
        arrayList.add(text);
        setChanged();
        notifyObservers();
    }

    public void addAll(int index, Collection<String> commandsReturned) {
        arrayList.addAll(index, commandsReturned);
        setChanged();
        notifyObservers();
    }

    public String remove(int index) {
        String elm = arrayList.remove(index);
        setChanged();
        notifyObservers();
        return elm;
    }

    public boolean isEmpty() {
        return arrayList.isEmpty();
    }

    public int size() {
        return arrayList.size();
    }

    public ArrayList<String> getCopyAsArrayList() {
        return new ArrayList<>(arrayList);
    }

    public String getAsJSONArrayString() {
        if (!arrayList.isEmpty()) {
            //limited to 100 Elements, for performance reasons
            return MCRJSONUtils.getJsonArray(arrayList.subList(0, 100 > arrayList.size() ? arrayList.size() : 100))
                .toString();
        }
        return "";
    }

    public void clear() {
        arrayList.clear();
        setChanged();
        notifyObservers();
    }
}
