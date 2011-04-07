/*
 * $Id$
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

import java.io.File;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;
import org.xml.sax.SAXParseException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRMODSCommands extends MCRAbstractCommands {

    private static final Logger LOGGER = Logger.getLogger(MCRMODSCommands.class);

    private static final String CMD_CLASS = MCRMODSCommands.class.getCanonicalName() + ".";

    public MCRMODSCommands() {
        super();
        command.add(new MCRCommand("load all mods documents from directory {0} for project {1}", CMD_CLASS + "loadFromDirectory String String",
            "Load all MODS documents as MyCoRe Objects for project {1} from directory {0}"));
        command.add(new MCRCommand("load mods document from file {0} for project {1}", CMD_CLASS + "loadFromFile String String",
            "Load MODS document {0} as MyCoRe Object for project {1}"));
    }

    public static List<String> loadFromDirectory(String directory, String projectID) {
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            throw new MCRException(MessageFormat.format("File {0} is not a directory.", directory));
        }
        String[] list = dir.list();
        if (list.length == 0) {
            LOGGER.warn("No files found in directory " + dir);
            return null;
        }
        LinkedList<String> cmds = new LinkedList<String>();
        for (String file : list) {
            if (file.endsWith(".xml")) {
                cmds.add(MessageFormat.format("load mods document from file {0} for project {1}", new File(dir, file).getAbsolutePath(), projectID));
            }
        }
        return cmds;
    }

    public static void loadFromFile(String modsFile, String projectID) throws SAXParseException, MCRPersistenceException, MCRActiveLinkException {
        File dir = new File(modsFile);
        if (!dir.isFile()) {
            throw new MCRException(MessageFormat.format("File {0} is not a file.", modsFile));
        }
        Document modsDoc = MCRXMLHelper.parseURI(dir.toURI(), true);
        if (!modsDoc.getRootElement().getNamespace().equals(MCRMODSWrapper.MODS_NS)) {
            throw new MCRException(MessageFormat.format("File {0} is not a MODS document.", modsFile));
        }
        MCRObject mcrObject = MCRMODSWrapper.wrapMODSDocument(modsDoc.getRootElement(), projectID);
        mcrObject.setId(MCRObjectID.getNextFreeId(mcrObject.getId().getBase()));
        MCRMetadataManager.create(mcrObject);
    }
}
