/*
 * $RCSfile$
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

package org.mycore.common.xml;

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
    public MCRXMLSortInterface addSortKey(Object sortKey, boolean order) throws MCRException;

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
