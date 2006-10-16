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

import org.jdom.Element;

import com.ibm.mm.sdk.common.DKAttrDefICM;
import com.ibm.mm.sdk.common.DKComponentTypeDefICM;
import com.ibm.mm.sdk.common.DKException;

/**
 * @author Thomas Scheffler (yagee)
 * @version $Revision$Date: 2006/09/08 10:21:00 $
 */
public class MCRCM8MetaISO8601Date extends MCRAbstractCM8ComponentType {
    private static final int TIMESTAMP_LENGTH = 26;

    public DKComponentTypeDefICM createComponentType(final Element element) throws DKException, Exception {
        final DKComponentTypeDefICM typeDef = getBaseItemTypeDef(element);
        typeDef.addAttr(getTimeStampAttr("mcrIso8601Date",
                "ISO-Norm:ISO 8601 : 1998 (E) Date; see http://www.w3.org/TR/NOTE-datetime"));
        typeDef.addAttr(getNoTSVarCharAttr("mcrIsoFormat", "DateFormat; see http://www.w3.org/TR/NOTE-datetime",
                TIMESTAMP_LENGTH));
        return typeDef;
    }

    /**
     * {@inheritDoc}
     * 
     * @return 'I'
     */
    protected char getDataTypeChar() {
        return 'I';
    }

    private DKAttrDefICM getTimeStampAttr(final String name, final String description) throws Exception, DKException {
        MCRCM8AttributeUtils.createAttributeTimestamp(getDsDefICM(), cutString(name, 15));
        final DKAttrDefICM attr = (DKAttrDefICM) getDsDefICM().retrieveAttr(name);
        attr.setNullable(true);
        attr.setDescription(description);
        attr.setUnique(false);
        return attr;
    }

}
