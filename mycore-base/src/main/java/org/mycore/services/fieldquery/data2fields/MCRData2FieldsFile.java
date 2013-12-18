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

import java.io.IOException;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.ifs.MCRFile;

public class MCRData2FieldsFile extends MCRIndexEntryBuilder {

    public MCRData2FieldsFile(String index, final MCRFile file) throws IOException {
        entry.setEntryID(file.getID());
        entry.setReturnID(getReturnID(file));

        slaves.add(new MCRData2FieldsFileMetadata(index, file));
        slaves.add(new MCRData2FieldsFileAdditionalData(index, file));
        slaves.add(new MCRData2FieldsFileXMLContent(index, file));
        slaves.add(new MCRData2FieldsFileTextFilter(index, file));
    }

    /** Maybe fieldquery is used in application without link table manager */
    private static boolean noLinkTableManager;

    static {
        MCRConfiguration config = MCRConfiguration.instance();
        noLinkTableManager = config.getString("MCR.Persistence.LinkTable.Store.Class", null) == null;
    }

    private String getReturnID(MCRFile file) {
        if (noLinkTableManager) {
            return file.getID();
        }

        String ownerID = file.getOwnerID();
        String returnID = MCRXMLFunctions.getMCRObjectID(ownerID, 10000);
        if (returnID == null) {
            return file.getID();
        }
        return returnID;

    }

}
