/*
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.frontend.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.transform.JDOMResult;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectStructure;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.mets.tools.MCRMetsResolver;
import org.mycore.mets.tools.MCRMetsSave;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRQueryParser;
import org.mycore.services.fieldquery.MCRResults;

/**
 * This class containing commands to administered mets.xml files in derivates
 * 
 * @author Frank Luetzenkirchen
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @author Stefan Freitag (sasf)
 * @version $Revision: 13085 $ $Date: 2008-02-06 18:27:24 +0100 (Mi, 06 Feb
 *          2008) $
 */
@MCRCommandGroup(name = "MCR METS Commands")
public final class MCRMetsModsCommands extends MCRAbstractCommands {

    private static Logger LOGGER = Logger.getLogger(MCRMetsModsCommands.class);

    /**
     * Remove mets.xml files from all derivates.
     * 
     * @throws Exception
     */
    @MCRCommand(syntax = "remove all mets.xml files", help = "Remove all mets.xml files.", order = 10)
    public static void removeMets() throws Exception {
        LOGGER.debug("Remove METS file start.");
        final long start = System.currentTimeMillis();
        List<String> derlist = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        for (String der : derlist) {
            MCRDirectory difs = MCRDirectory.getRootDirectory(der);
            if (difs != null) {
                MCRFilesystemNode mets = difs.getChild("mets.xml");
                if (mets != null) {
                    LOGGER.info("Mets file found on " + der);
                    mets.delete();
                }
            }
        }
        LOGGER.debug("Remove METS file request took " + (System.currentTimeMillis() - start) + "ms.");
    }

    /**
     * Remove mets.xml files from derivates refernced by objects with a given MCRType.
     * 
     * @throws Exception
     */
    @MCRCommand(syntax = "remove mets.xml files for object type {0}", help = "Remove mets.xml files for object type {0}.", order = 20)
    public static void removeMetsForType(String type) {
        LOGGER.debug("Remove METS file for " + type + " start.");
        final long start = System.currentTimeMillis();

        MCRQueryCondition fromcond = new MCRQueryCondition("objectType", "=", type);
        MCRQuery fromquery = new MCRQuery(fromcond);
        MCRResults fromresult = MCRQueryManager.search(fromquery);

        for (MCRHit fromhit : fromresult) {
            String fromid = fromhit.getID();

            MCRObject fromobj = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(fromid));
            MCRObjectStructure structure = fromobj.getStructure();
            List<MCRMetaLinkID> derivates = structure.getDerivates();
            for (MCRMetaLinkID derivate : derivates) {
                MCRObjectID mcrderid = derivate.getXLinkHrefID();
                MCRDerivate mcrder = MCRMetadataManager.retrieveMCRDerivate(mcrderid);
                MCRDirectory derdir = mcrder.receiveDirectoryFromIFS();
                if (derdir.getChild("mets.xml") != null) {
                    LOGGER.info("Removing mets.xml from Derivate " + mcrderid + " linked with Object " + fromid);
                    derdir.getChild("mets.xml").delete();
                }

            }

        }

        LOGGER.debug("Remove METS file for " + type + " request took " + (System.currentTimeMillis() - start) + "ms.");
    }

    public static void fixExistingMets() throws Exception {
        MCRQuery q = new MCRQuery(new MCRQueryParser().parse("objectsWithURN = true"));
        MCRResults result = MCRQueryManager.search(q);

        for (MCRHit hit : result) {
            MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(hit.getID()));
            MCRDirectory difs = MCRDirectory.getRootDirectory(der.getId().toString());
            if (difs != null) {
                MCRFilesystemNode mets = difs.getChild("mets.xml");
                if (mets == null) {

                    LOGGER.info("URN Derivate without mets :" + der);
                } else {
                    LOGGER.info("URN Derivate with mets :" + der);

                    Map<String, String> urnFileMap = der.getUrnMap();
                    if (urnFileMap.size() > 0) {
                        MCRMetsSave.updateMetsOnUrnGenerate(der.getId(), urnFileMap);
                    }
                }
            }
        }
    }

    /**
     * This method adds the given file to the mets document. If the filename is
     * already present in the mets file nothing is done. If there no mets file
     * within the given derivate nothing is done.
     * 
     * @param filename
     *            the name of the file to add
     * @param derivate
     *            the derivate where the mets file is located
     */
    @MCRCommand(syntax = "add file {0} to mets document in {1}",
            help = "Adds the given file with name {0} to the mets document contained in derivate {1}.", order = 80)
    public static void addFileToMets(String filename, String derivate) {
        LOGGER.info("Adding file " + filename + " to mets file in " + derivate);
        if (filename == null || filename.length() == 0 || derivate == null || derivate.length() == 0) {
            LOGGER.warn("Invalid parameters. Null is not allowd");
            return;
        }
        if (!MCRMetsModsCommands.exist(filename, derivate)) {
            LOGGER.info("File \"" + filename + "\" is not a member of derivate \"" + derivate + "\"");
            return;
        }

        try {
            if (MCRMetsModsCommands.existsInMets(filename, derivate)) {
                LOGGER.info("File \"" + filename + "\" is already part of the mets document");
                return;
            }
        } catch (Exception ex) {
            LOGGER.error("Error while checking mets for file " + filename, ex);
            return;
        }

        try {

            MCRMetsSave.updateMetsOnFileAdd(MCRFile.getMCRFile(MCRObjectID.getInstance(derivate), filename));
        } catch (Exception ex) {
            LOGGER.error("Error while updating mets file", ex);
        }
    }

    /**
     * @return true if the file is already in the mets file or false otherwise
     */
    private static boolean existsInMets(String filename, String derivate) throws Exception {
        MCRMetsResolver resolver = new MCRMetsResolver();
        Document mets = null;

        Source source = resolver.resolve("mets:" + derivate, null);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        JDOMResult jdomResult = new JDOMResult();
        transformerFactory.newTransformer().transform(source, jdomResult);
        mets = jdomResult.getDocument();

        if (mets == null) {
            return false;
        }
        ArrayList<Namespace> al = new ArrayList<Namespace>();
        al.add(MCRConstants.METS_NAMESPACE);
        al.add(MCRConstants.XLINK_NAMESPACE);
        XPathFactory xpf = XPathFactory.instance();
        XPathExpression<Element> obj = xpf.compile(
                "mets:mets/mets:fileSec/mets:fileGrp[@USE='MASTER']/mets:file/mets:FLocat[@xlink:href='" + filename
                        + "']", Filters.element(), null, al);
        return obj != null;

    }

    /**
     * Checks whether the given file exists in the derivate.
     * 
     * @param filename
     *            the name of the file to add
     * @param derivate
     *            the derivate where the mets file is located
     */
    private static boolean exist(String filename, String derivate) {
        MCRDerivate derObj = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(derivate));
        return derObj.receiveDirectoryFromIFS().hasChild(filename);
    }
}
