/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package mycore.cm8;

import com.ibm.mm.sdk.server.*;
import com.ibm.mm.sdk.common.*;
import mycore.common.MCRPersistenceException;

/**
 * This interface is designed to choose the datamodel classes for the
 * CM8 persistence layer
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/

public interface MCRCM8MetaInterface
{

/**
 * This method create a DKComponentTypeDefICM to create a complete
 * ItemType from the configuration.
 *
 * @param element  a MCR datamodel element as JDOM Element
 * @return a DKComponentTypeDefICM for the MCR datamodel element
 * @exception MCRPersistenceException a general Exception of MyCoRe CM8
 **/
public DKComponentTypeDefICM createItemType(org.jdom.Element je,
  DKDatastoreICM connection, DKDatastoreDefICM dsDefICM, String prefix)
  throws MCRPersistenceException;

}
