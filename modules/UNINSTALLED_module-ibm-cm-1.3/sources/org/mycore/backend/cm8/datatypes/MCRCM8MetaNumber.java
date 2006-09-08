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

import org.mycore.datamodel.metadata.MCRMetaDefault;

import com.ibm.mm.sdk.common.DKComponentTypeDefICM;
import com.ibm.mm.sdk.common.DKException;

/**
 * This class implements the interface for the CM8 persistence layer for the
 * data model type MetaNumber.
 * 
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRCM8MetaNumber extends MCRAbstractCM8ComponentType {

    public DKComponentTypeDefICM createComponentType(final org.jdom.Element element) throws DKException, Exception {
        final DKComponentTypeDefICM typeDef = getBaseItemTypeDef(element);
        typeDef.addAttr(getNoTSVarCharAttr("mcrNumber", "Value of MCRNumber", MCRMetaDefault.DEFAULT_STRING_LENGTH));
        typeDef.addAttr(getNoTSVarCharAttr("mcrDimension", "Dimension of a number",
                org.mycore.datamodel.metadata.MCRMetaNumber.MAX_DIMENSION_LENGTH));
        typeDef.addAttr(getNoTSVarCharAttr("mcrMeasurement", "Measurement of a number",
                org.mycore.datamodel.metadata.MCRMetaNumber.MAX_MEASUREMENT_LENGTH));
        return typeDef;
    }

    /**
     * {@inheritDoc}
     * 
     * @return 'N'
     */
    protected char getDataTypeChar() {
        return 'N';
    }
}
