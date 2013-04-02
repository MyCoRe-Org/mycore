package org.mycore.util.concurrent;

/**
 * Objects can implement this interface if they are capable of being prioritized.
 * 
 * @author Matthias Eichner
 */
public interface Prioritizable<T> {

    /**
     * Returns the priority.
     */
    public T getPriority();

}
