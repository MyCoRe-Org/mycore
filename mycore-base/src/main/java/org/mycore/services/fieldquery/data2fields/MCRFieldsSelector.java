/*
 * $Revision$ 
 * $Date$
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

package org.mycore.services.fieldquery.data2fields;

import org.mycore.datamodel.ifs.MCRFile;

public class MCRFieldsSelector {

    private String index;

    private String objectType;

    private String sourceType;

    protected MCRFieldsSelector(String index, String objectType, String sourceType) {
        this.index = index;
        this.objectType = objectType;
        this.sourceType = sourceType;
    }

    public String getIndex() {
        return index;
    }

    public String getObjectType() {
        return objectType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getKey() {
        return index + "/" + sourceType + "/" + objectType;
    }
}

class MCRFieldsSelectorBase extends MCRFieldsSelector {

    public MCRFieldsSelectorBase(String index, String objectType, String sourceType) {
        super(index, objectType, sourceType);
    }
}

class MCRFieldsSelectorFile extends MCRFieldsSelector {

    public MCRFieldsSelectorFile(String index, MCRFile file, String sourceType) {
        super(index, file.getContentTypeID(), sourceType);
    }
}
