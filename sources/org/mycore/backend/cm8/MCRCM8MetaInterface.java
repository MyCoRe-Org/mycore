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

package org.mycore.backend.cm8;

import org.mycore.common.MCRPersistenceException;

/**
 * This interface is designed to choose the datamodel classes for the CM8
 * persistence layer
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public interface MCRCM8MetaInterface {
    /**
     * This method create a DKComponentTypeDefICM to create a complete ItemType
     * from the configuration.
     * 
     * @param element
     *            a MCR datamodel element as JDOM Element
     * @param connection
     *            the connection to the CM8 datastore
     * @param dsDefICM
     *            the datastore definition
     * @param prefix
     *            the prefix name for the item type
     * @param textindex
     *            the definition of the text search index
     * @param textserach
     *            the flag to use textsearch as string
     * @return a DKComponentTypeDefICM for the MCR datamodel element
     * @exception MCRPersistenceException
     *                a general Exception of MyCoRe CM8
     */
    public DKComponentTypeDefICM createItemType(org.jdom.Element element, DKDatastoreICM connection, DKDatastoreDefICM dsDefICM, String prefix, DKTextIndexDefICM textindex, String textsearch) throws MCRPersistenceException;
}
