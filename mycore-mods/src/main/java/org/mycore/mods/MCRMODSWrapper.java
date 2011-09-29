/*
 * $Revision: 5697 $ $Date: 07.04.2011 $
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

package org.mycore.mods;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import org.jdom.Element;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRMetaXML;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectMetadata;

/**
 * @author Frank L\u00FCtzenkirchen
 * @author Thomas Scheffler
 */
public class MCRMODSWrapper {

    private static final String MODS_CONTAINER = "modsContainer";

    private static final String DEF_MODS_CONTAINER = "def.modsContainer";

    public static final String MODS_OBJECT_TYPE = "mods";

    private static final String MODS_DATAMODEL = "datamodel-mods.xsd";

    public static MCRObject wrapMODSDocument(Element modsDefinition, String projectID) {
        MCRMODSWrapper wrapper = new MCRMODSWrapper();
        wrapper.setID(projectID, 0);
        wrapper.setMODS(modsDefinition);
        return wrapper.getMCRObject();
    }

    private MCRObject object;

    private Element mods;

    public MCRMODSWrapper() {
        object = new MCRObject();
        object.setSchema(MODS_DATAMODEL);
    }

    public MCRObject getMCRObject() {
        return object;
    }

    public MCRObjectID setID(String projectID, int ID) {
        MCRObjectID objID = MCRObjectID.getInstance(MessageFormat.format("{0}_{1}_{2}", projectID, MODS_OBJECT_TYPE, ID));
        object.setId(objID);
        return objID;
    }

    public void setMODS(Element mods) {
        MCRObjectMetadata om = object.getMetadata();
        if (om.getMetadataElement(DEF_MODS_CONTAINER) != null)
            om.removeMetadataElement(DEF_MODS_CONTAINER);

        MCRMetaXML modsContainer = new MCRMetaXML(MODS_CONTAINER, null, 0);
        List<MCRMetaXML> list = Collections.nCopies(1, modsContainer);
        MCRMetaElement defModsContainer = new MCRMetaElement(MCRMetaXML.class, DEF_MODS_CONTAINER, false, true, list);
        om.setMetadataElement(defModsContainer);

        this.mods = mods;
        modsContainer.addContent(mods);
    }

    public Element getMODS() {
        return mods;
    }
}
