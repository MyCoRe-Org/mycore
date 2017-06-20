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
        this.arrayList = new CopyOnWriteArrayList<String>();
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
        return new ArrayList<String>(arrayList);
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
