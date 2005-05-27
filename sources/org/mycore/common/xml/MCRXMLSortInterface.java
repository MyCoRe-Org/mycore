package org.mycore.common.xml;

import java.net.ServerSocket;

import javax.servlet.ServletContext;

import org.mycore.common.MCRException;

/**
 * This interface should be implemented by a class that want to sort sertain
 * Objects used my any MyCoRe class.
 * 
 * @author Thomas Scheffler
 * @version $Revision$ $Date$
 */
public interface MCRXMLSortInterface {

    /**
     * Variable for sorting order.
     */
    public static final boolean INORDER = true;

    /**
     * Variable for sorting order.
     */
    public static final boolean REVERSE = false;

    /**
     * Adds a key as a sort criteria. Sorting order in this case should always
     * be <code>INORDER</code>. The implementation of this Interface must
     * also know how to handle the Object.
     * 
     * @return must return itself
     * @param sortKey
     *            Key for sorting
     * @throws MCRException
     *             if doesn't know how to handle <code>sortKey</code>
     * @see #INORDER variable for sorting order
     * @author Thomas Scheffler
     */
    public MCRXMLSortInterface addSortKey(Object sortKey) throws MCRException;

    /**
     * Adds a key as a sort criteria with a specified sorting order. The
     * implementation of this Interface must know how to handle the Object.
     * 
     * @return must return itself
     * @param sortKey
     *            Key for sorting
     * @param order
     *            sorting Order
     * @throws MCRException
     *             if doesn't know how to handle <code>sortKey</code>
     * @see #INORDER variable for sorting order
     * @author Thomas Scheffler
     */
    public MCRXMLSortInterface addSortKey(Object sortKey, boolean order)
            throws MCRException;

    /**
     * Adds an array of Objects to be sorted. The implementation of this
     * Interface must know how to handle each Object.
     * 
     * @return must return itself
     * @throws MCRException
     *             if doesn't know how to handle <code>sortObjects</code>
     * @param sortObjects
     *            Objects to be sorted
     * @author Thomas Scheffler
     */
    public MCRXMLSortInterface add(Object[] sortObjects) throws MCRException;

    /**
     * Adds an Object to be sorted. The implementation of this Interface must
     * know how to handle each Object.
     * 
     * @return must return itself
     * @throws MCRException
     *             if doesn't know how to handle <code>sortObject</code>
     * @param sortObject
     *            Objects to be sorted
     * @author Thomas Scheffler
     */
    public MCRXMLSortInterface add(Object sortObject) throws MCRException;

    /**
     * sorts the embeded Objects in the given sorting order.
     * 
     * @return Object[] sorted Array of Objects.
     * @throws MCRException
     *             if sorting fails
     * @author Thomas Scheffler
     */
    public Object[] sort() throws MCRException;

    /**
     * sorts the embeded Objects in the given sorting order.
     * 
     * @return Object[] sorted Array of Objects.
     * @param reversed
     *            true if given sorting Order should be reversed
     * @throws MCRException
     *             if sorting fails
     * @author Thomas Scheffler
     */
    public Object[] sort(boolean reversed) throws MCRException;

    /**
     * removes all Objects to be sorted
     * 
     * @author Thomas Scheffler
     */
    public void clearObjects();

    /**
     * removes all sorting keys
     * 
     * @author Thomas Scheffler
     */
    public void clearSortKeys();

    /**
     * sets Servlet-Context
     * 
     * @param context
     *            ServletContext
     */
    public void setServletContext(ServletContext context);

    /**
     * gets Servlet-Context
     * 
     * @return ServletContext implementing ServletContext
     */
    public ServletContext getServletContext();

}