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

package org.mycore.backend.cm8.datatypes;

import com.ibm.mm.sdk.common.DKComponentTypeDefICM;
import com.ibm.mm.sdk.common.DKDatastoreDefICM;
import com.ibm.mm.sdk.common.DKException;

/**
 * This interface is designed to choose the datamodel classes for the CM8
 * persistence layer
 * 
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public interface MCRCM8ComponentType {
    /**
     * This method create a DKComponentTypeDefICM to create a complete ItemType
     * from the configuration.
     * 
     * @param element
     *            a MCR datamodel element as JDOM Element
     * @throws DKException
     *             from underlying ContentManager processes
     * @throws Exception
     *             from underlying ContentManager processes
     */
    public DKComponentTypeDefICM createComponentType(org.jdom.Element element) throws DKException, Exception;

    /**
     * sets ContentManager store definition
     * 
     * @param dsDefICM
     *            the dataStore definition to set
     */
    public void setDsDefICM(DKDatastoreDefICM dsDefICM);

    /**
     * @param componentNamePrefix
     *            the componentNamePrefix to set
     */
    public void setComponentNamePrefix(String componentNamePrefix);

}
