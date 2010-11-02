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

package org.mycore.oai;

import org.apache.log4j.Logger;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRQueryCondition;

public class MCROAIAdapterMyCoRe extends MCROAIAdapter {
    private final static Logger LOGGER = Logger.getLogger(MCROAIAdapterMyCoRe.class);

    public boolean exists(String id) {
        try {
            MCRObjectID oid = MCRObjectID.getInstance(id);
            return MCRXMLMetadataManager.instance().exists(oid);
        } catch (Exception ex) {
            String msg = "Exception while checking existence of object " + id;
            LOGGER.warn(msg, ex);
            return false;
        }
    }

    public MCRCondition buildSetCondition(String setSpec) {
        String categID = setSpec.substring(setSpec.lastIndexOf(':') + 1).trim();
        String classID = setSpec.substring(0, setSpec.indexOf(':')).trim();
        String id = classID + ":" + categID;
        return new MCRQueryCondition(MCRFieldDef.getDef("category"), "=", id);
    }
}
