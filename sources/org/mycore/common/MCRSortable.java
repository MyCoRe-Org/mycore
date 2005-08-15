package org.mycore.common;

import org.mycore.common.xml.MCRXMLSortInterface;

/**
 * This Interface should be Implemented when a Class has Objects to be sorted by
 * an implementation of MCRXMLSortInterface
 * 
 * @see org.mycore.common.xml.MCRXMLSortInterface MCRXMLSortInterface
 * @author Thomas Scheffler
 * @version $Revision$ $Date$
 */
public interface MCRSortable {

	/**
	 * sorts some Objects of implementing class.
	 * 
	 * @param sorter
	 *            Object responsible for sorting
	 * @throws MCRException
	 *             if sorting fails somehow
	 * @author Thomas Scheffler
	 */
	public void sort(MCRXMLSortInterface sorter) throws MCRException;

	/**
	 * 
	 * @param sorter
	 *            Object responsible for sorting
	 * @param order
	 *            true if reversed sorting order
	 * @see MCRXMLSortInterface#sort(boolean) sorting method.
	 * @throws MCRException
	 *             if sorting fails somehow
	 * @author Thomas Scheffler
	 */
	public void sort(MCRXMLSortInterface sorter, boolean reversed)
			throws MCRException;

}