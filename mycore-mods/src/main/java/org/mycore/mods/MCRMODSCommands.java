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
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.mods.rss.MCRRSSFeedImporter;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@MCRCommandGroup(name = "MODS Commands")
public class MCRMODSCommands extends MCRAbstractCommands {

    public static final String MODS_V3_XSD_URI = "http://www.loc.gov/standards/mods/v3/mods-3-6.xsd";

    private static final Logger LOGGER = LogManager.getLogger(MCRMODSCommands.class);

    @MCRCommand(syntax = "load all mods documents from directory {0} for project {1}",
        help = "Load all MODS documents as MyCoRe Objects for project {1} from directory {0}",
        order = 10)
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
        return Arrays.stream(list).filter(file -> file.endsWith(".xml"))
            .map(file -> MessageFormat.format("load mods document from file {0} for project {1}",
                new File(dir, file).getAbsolutePath(), projectID))
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "load mods document from file {0} for project {1}",
        help = "Load MODS document {0} as MyCoRe Object for project {1}",
        order = 20)
    public static void loadFromFile(String modsFileName, String projectID) throws JDOMException, IOException,
        MCRActiveLinkException, SAXException, MCRPersistenceException, MCRAccessException {
        File modsFile = new File(modsFileName);
        if (!modsFile.isFile()) {
            throw new MCRException(MessageFormat.format("File {0} is not a file.", modsFile.getAbsolutePath()));
        }
        SAXBuilder s = new SAXBuilder(XMLReaders.NONVALIDATING, null, null);
        Document modsDoc = s.build(modsFile);
        //force validation against MODS XSD
        MCRXMLHelper.validate(modsDoc, MODS_V3_XSD_URI);
        Element modsRoot = modsDoc.getRootElement();
        if (!modsRoot.getNamespace().equals(MCRConstants.MODS_NAMESPACE)) {
            throw new MCRException(
                MessageFormat.format("File {0} is not a MODS document.", modsFile.getAbsolutePath()));
        }
        if (modsRoot.getName().equals("modsCollection")) {
            List<Element> modsElements = modsRoot.getChildren("mods", MCRConstants.MODS_NAMESPACE);
            for (Element mods : modsElements) {
                saveAsMyCoReObject(projectID, mods);
            }
        } else {
            saveAsMyCoReObject(projectID, modsRoot);
        }
    }

    private static void saveAsMyCoReObject(String projectID, Element modsRoot)
        throws MCRActiveLinkException, MCRPersistenceException, MCRAccessException {
        MCRObject mcrObject = MCRMODSWrapper.wrapMODSDocument(modsRoot, projectID);
        mcrObject.setId(MCRObjectID.getNextFreeId(mcrObject.getId().getBase()));
        MCRMetadataManager.create(mcrObject);
    }

    @MCRCommand(syntax = "import publications from {0} RSS feed for project {1}",
        help = "Read RSS feed from data source {0}, convert and save new publications as MyCoRe Object for project {1}",
        order = 30)
    public static void importFromRSSFeed(String sourceSystemID, String projectID) throws Exception {
        MCRRSSFeedImporter.importFromFeed(sourceSystemID, projectID);
    }
}
