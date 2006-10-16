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

import com.ibm.mm.sdk.common.DKAttrDefICM;
import com.ibm.mm.sdk.common.DKComponentTypeDefICM;
import com.ibm.mm.sdk.common.DKException;

/**
 * This class implements the interface for the CM8 persistence layer for the
 * data model type MetaInstitutionName.
 * 
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRCM8MetaIFS extends MCRAbstractCM8ComponentType {

    private static final int IFS_ID_LENGTH = 16;

    public DKComponentTypeDefICM createComponentType(final org.jdom.Element element) throws DKException, Exception {
        final int attrSize = getAttrSize(element, true);
        final DKComponentTypeDefICM typeDef = getBaseItemTypeDef(element);
        typeDef.addAttr(getVarCharAttr("mcrSource", "Source Path of the MyCoRe derivate", attrSize));
        typeDef.addAttr(getVarCharAttr("mcrMainDoc", "relative path to the main document of the derivate", attrSize));
        final DKAttrDefICM attr = getNoTSVarCharAttr("mcrIfsId", "ID ", IFS_ID_LENGTH);
        attr.setUnique(true);
        attr.setNullable(false);
        typeDef.addAttr(attr);
        return typeDef;
    }

    /**
     * {@inheritDoc}
     * 
     * @return 'e'
     */
    protected char getDataTypeChar() {
        return 'e';
    }
}
