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
     * @param reversed
     *            true if reversed sorting order
     * @see MCRXMLSortInterface#sort(boolean) sorting method.
     * @throws MCRException
     *             if sorting fails somehow
     * @author Thomas Scheffler
     */
    public void sort(MCRXMLSortInterface sorter, boolean reversed) throws MCRException;
}
