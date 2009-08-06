package org.mycore.importer;

/**
 * This interface creates a connection to the import source
 * location. This could be a database, a xml file, a webpage or 
 * something else.
 * 
 * @author Matthias Eichner
 *
 * @param <C> the retrieving data
 */
public interface MCRImportConnector<C> {

    /**
     * Creates the connection to the import source.
     * 
     * @return the created connection.
     */
    public C connect();

    /**
     * Closes the connection.
     */
    public void close();

}