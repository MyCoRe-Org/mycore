package org.mycore.importer;


/**
 * This interface creates a connection to the import source
 * location. This could be a database, a xml file, a webpage or 
 * something else. With the retrieve method its possible to
 * get the data from this connection. 
 * 
 * @author Matthias Eichner
 *
 * @param <T> the retrieving data
 */
public interface MCRImportRetriever<T> {

    /**
     * Creates the connection to the import source.
     * 
     * @return the created connection.
     */
    public void connect();

    /**
     * Closes the connection.
     */
    public void close();

    /**
     * Retrieves the data from the connection.
     * 
     * @return the data.
     */
    public T retrieve(); 
}