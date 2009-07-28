package org.mycore.frontend.redundancy;

/**
 * The MCRRedundancyFormattable interface could be used to perform
 * custom object formatting.
 * 
 * @author Matthias Eichner
 */
public interface MCRRedundancyFormattable<T> {

    public T format(T stringToFormat);

}