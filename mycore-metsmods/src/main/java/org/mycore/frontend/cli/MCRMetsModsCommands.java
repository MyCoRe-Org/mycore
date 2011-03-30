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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMResult;
import org.jdom.xpath.XPath;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRUtils;
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
import org.mycore.frontend.metsmods.MCRMetsModsUtil;
import org.mycore.mets.tools.MCRMetsResolver;
import org.mycore.mets.tools.MCRMetsSave;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryManager;
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
public final class MCRMetsModsCommands extends MCRAbstractCommands {

    private static Logger LOGGER = Logger.getLogger(MCRMetsModsCommands.class.getName());

    private static MCRConfiguration CONFIG = MCRConfiguration.instance();

    private static String known_image_list = CONFIG.getString("MCR.Component.MetsMods.allowed", "");

    private static String metsfile = CONFIG.getString("MCR.MetsMots.ConfigFile", "mets.xml");

    private static String activated = CONFIG.getString("MCR.Component.MetsMods.activated", "");

    /**
     * The empty constructor.
     */
    public MCRMetsModsCommands() {
        super();

        MCRCommand com = null;

        com = new MCRCommand("build mets files for type {0}", "org.mycore.frontend.cli.MCRMetsModsCommands.buildMetsForType String",
                "Create the mets.xml file in the derivate directory of given type.");
        command.add(com);

        com = new MCRCommand("build mets files for Object {0}",
                "org.mycore.frontend.cli.MCRMetsModsCommands.buildMetsForMCRObjectID String",
                "Create the mets.xml file for all derivates for the given object ID.");
        command.add(com);

        com = new MCRCommand("build mets files for Derivate {0}",
                "org.mycore.frontend.cli.MCRMetsModsCommands.buildMetsForMCRDerivateID String",
                "Create the mets.xml file in the derivate of the given derivate ID.");
        command.add(com);

        com = new MCRCommand("build mets files", "org.mycore.frontend.cli.MCRMetsModsCommands.buildMets",
                "Create the mets.xml file in the derivate directory.");
        command.add(com);

        com = new MCRCommand("check mets files for type {0} with exclude label {1}",
                "org.mycore.frontend.cli.MCRMetsModsCommands.checkMetsForType String String", "Check mets files for given type.");
        command.add(com);

        com = new MCRCommand("check mets files for type {0}", "org.mycore.frontend.cli.MCRMetsModsCommands.checkMetsForType String",
                "Check mets files for given type.");
        command.add(com);

        com = new MCRCommand("check mets files for Object {0} with exclude label {1}",
                "org.mycore.frontend.cli.MCRMetsModsCommands.checkMetsForMCRObjectID String String", "Check mets files for given object.");
        command.add(com);

        com = new MCRCommand("check mets files for Object {0}",
                "org.mycore.frontend.cli.MCRMetsModsCommands.checkMetsForMCRObjectID String", "Check mets files for given object ID.");
        command.add(com);

        com = new MCRCommand("check mets files for Derivate {0}",
                "org.mycore.frontend.cli.MCRMetsModsCommands.checkMetsForMCRDerivateID String", "Check mets files for given derivate ID.");
        command.add(com);

        com = new MCRCommand("check mets files", "org.mycore.frontend.cli.MCRMetsModsCommands.checkMets", "Check the mets.xml file.");
        command.add(com);

        com = new MCRCommand("remove mets files", "org.mycore.frontend.cli.MCRMetsModsCommands.removeMets", "Remove all mets files.");
        command.add(com);

        com = new MCRCommand("add file {0} to mets document in {1}",
                "org.mycore.frontend.cli.MCRMetsModsCommands.addFileToMets String String",
                "Adds the given file with name {0} to the mets document contained in derivate {1}.");
        command.add(com);
    }

    /**
     * The command build mets.xml files in the derivates if it does not exist
     * and the content are images.
     */
    public static final void buildMets() throws Exception {
        LOGGER.info("Build all METS files start.");
        final long start = System.currentTimeMillis();
        List<String> derlist = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        for (String der : derlist) {
            buildMetsForMCRDerivateID(der);
        }
        LOGGER.info("Build all METS files took " + (System.currentTimeMillis() - start) + "ms.");
    }

    /*
     * The command look for derivates with images and build mets.xml files if it
     * not exist for all MCRObjects of the given type.
     * 
     * @param type the data type of the MCRObjectIDs
     */
    public static final void buildMetsForType(String type) {
        LOGGER.info("Build METS file for type " + type + " start.");
        final long start = System.currentTimeMillis();
        MCRQueryCondition fromcond = new MCRQueryCondition(MCRFieldDef.getDef("objectType"), "=", type);
        MCRQuery fromquery = new MCRQuery(fromcond);
        MCRResults fromresult = MCRQueryManager.search(fromquery);
        Iterator<MCRHit> fromiter = fromresult.iterator();
        while (fromiter.hasNext()) {
            MCRHit fromhit = fromiter.next();
            String fromid = fromhit.getID();
            buildMetsForMCRObjectID(fromid);

        }
        LOGGER.info("Build METS file for type " + type + " took " + (System.currentTimeMillis() - start) + "ms.");
    }

    /*
     * The command look for derivates with images and build mets.xml files if it
     * not exist for a given MCRObject.
     * 
     * @param MCRID the MCRObjectID to check and build
     */
    public static final void buildMetsForMCRObjectID(String MCRID) {
        LOGGER.info("Build METS file for ID " + MCRID + " start.");
        final long start = System.currentTimeMillis();
        MCRObject mcrobj = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(MCRID));
        MCRObjectStructure structure = mcrobj.getStructure();
        List<MCRMetaLinkID> derivates = structure.getDerivates();
        for (int i = 0; i < derivates.size(); i++) {
            MCRMetaLinkID mcrder = derivates.get(i);
            LOGGER.debug("For ID " + MCRID + " found derivate " + mcrder.getXLinkHref());
            buildMetsForMCRDerivateID(mcrder.getXLinkHrefID().toString());
        }
        LOGGER.debug("Build METS file for ID " + MCRID + " took " + (System.currentTimeMillis() - start) + "ms.");
    }

    /*
     * The command build a mets.xml file for a given MCRDerivate if it does not
     * exist and the derivate contains images.
     * 
     * @param MCRID the MCRObjectID
     */
    public static final void buildMetsForMCRDerivateID(String MCRID) {
        String baseurl = MCRConfiguration.instance().getString("MCR.baseurl", "http://127.0.0.1:8080");
        LOGGER.debug("Build METS file for ID " + MCRID + "  start.");
        final long start = System.currentTimeMillis();
        MCRDerivate derxml = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(MCRID));
        MCRObjectID docid = derxml.getDerivate().getMetaLink().getXLinkHrefID();
        MCRDirectory difs = MCRDirectory.getRootDirectory(MCRID);
        if (difs != null) {
            MCRFilesystemNode mets = difs.getChild("mets.xml");
            if (mets == null) {
                LOGGER.warn("No mets.xml file was found for " + docid.toString() + " in " + MCRID + ".");
                MCRFilesystemNode[] fsnode = difs.getChildren();
                boolean checkimage = false;
                for (int i = 0; i < fsnode.length; i++) {
                    StringTokenizer st = new StringTokenizer(known_image_list, ",");
                    while (st.hasMoreElements()) {
                        if (fsnode[i].getName().endsWith(st.nextToken())) {
                            checkimage = true;
                            break;
                        }
                    }
                    if (checkimage)
                        break;
                }
                if (checkimage) {
                    LOGGER.debug("Start create mets.xml");
                    ArrayList<String> pic_list = new ArrayList<String>();
                    addPicturesToList(difs, pic_list);
                    Collections.sort(pic_list);

                    MCRMetsModsUtil mmu = new MCRMetsModsUtil();
                    Element new_mets_file = mmu.createNewMetsFile(MCRID);
                    if (activated.contains("CONTENTIDS"))
                        new_mets_file = mmu.createMetsElement(pic_list, new_mets_file, baseurl + "servlets/MCRFileNodeServlet", baseurl
                                + "receive/" + docid);
                    else
                        new_mets_file = mmu.createMetsElement(pic_list, new_mets_file, baseurl + "servlets/MCRFileNodeServlet");

                    if (LOGGER.isDebugEnabled()) {
                        MCRUtils.writeElementToSysout(new_mets_file);
                    }
                    try {
                        XMLOutputter xmlout = new XMLOutputter(Format.getPrettyFormat());
                        String full_mets = xmlout.outputString(new_mets_file);
                        LOGGER.debug("storing new mets file...");
                        MCRFile file = new MCRFile(metsfile, difs);
                        ByteArrayInputStream bais = new ByteArrayInputStream(full_mets.getBytes());
                        long sizeDiff = file.setContentFrom(bais, false);
                        file.storeContentChange(sizeDiff);
                    } catch (Exception e) {
                        if (LOGGER.isDebugEnabled()) {
                            e.printStackTrace();
                        } else {
                            LOGGER.error("Error while storing new mets file...", e);
                        }
                    }
                }
            }
        } else {
            LOGGER.debug("mets.xml exist");
        }
        LOGGER.debug("Build METS file for ID " + MCRID + " took " + (System.currentTimeMillis() - start) + "ms.");
    }

    public static void checkMetsForType(String type) {
        checkMetsForType(type, null);
    }

    public static void checkMetsForType(String type, String exclude) {
        LOGGER.info("Check METS file for type " + type + " start.");
        final long start = System.currentTimeMillis();

        MCRQueryCondition fromcond = new MCRQueryCondition(MCRFieldDef.getDef("objectType"), "=", type);
        MCRQuery fromquery = new MCRQuery(fromcond);
        MCRResults fromresult = MCRQueryManager.search(fromquery);
        Iterator<MCRHit> fromiter = fromresult.iterator();

        while (fromiter.hasNext()) {
            MCRHit fromhit = fromiter.next();
            String fromid = fromhit.getID();

            if (exclude == null)
                checkMetsForMCRObjectID(fromid, null);
            else {
                StringTokenizer excluder = new StringTokenizer(exclude, ",");
                while (excluder.hasMoreTokens()) {
                    checkMetsForMCRObjectID(fromid, excluder.nextToken());
                }

            }

        }

        LOGGER.debug("Check METS file for type " + type + " request took " + (System.currentTimeMillis() - start) + "ms.");
    }

    /**
     * The command check a object for a given ID for mets.xml.
     * 
     * @param MCRID
     *            the MCRObjectID
     */
    public static void checkMetsForMCRObjectID(String MCRID) {
        checkMetsForMCRObjectID(MCRID, null);
    }

    /**
     * The command check a derivate for a given ID for mets.xml.
     * 
     * @param MCRID
     *            the MCRObjectID
     * @param excludelabel
     *            the label to exclude dataset of the derivate refernce in the
     *            object
     */
    public static void checkMetsForMCRObjectID(String MCRID, String excludelabel) {
        LOGGER.info("Check METS file for ID " + MCRID + " start.");
        final long start = System.currentTimeMillis();

        MCRObject mcrobj = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(MCRID));
        MCRObjectStructure structure = mcrobj.getStructure();
        List<MCRMetaLinkID> derivates = structure.getDerivates();
        for (int i = 0; i < derivates.size(); i++) {
            MCRMetaLinkID mcrder = derivates.get(i);
            LOGGER.debug("found derivate " + mcrder.getXLinkTitle());
            String label = mcrder.getXLinkLabel();

            if (excludelabel == null)
                checkMetsForMCRDerivateID(mcrder.getXLinkHrefID().toString());
            else {
                if (!label.contains(excludelabel))
                    checkMetsForMCRDerivateID(mcrder.getXLinkHrefID().toString());
            }
        }
        LOGGER.debug("Check METS file request took " + (System.currentTimeMillis() - start) + "ms.");
    }

    /**
     * The command check a derivate for a given ID for mets.xml.
     * 
     * @param MCRID
     *            the MCRObjectID
     */
    public static void checkMetsForMCRDerivateID(String MCRID) {
        LOGGER.info("Check METS file for ID " + MCRID + "  start.");
        final long start = System.currentTimeMillis();
        MCRDirectory difs = MCRDirectory.getRootDirectory(MCRID);
        if (difs != null) {
            MCRFilesystemNode mets = difs.getChild("mets.xml");
            if (mets == null) {
                LOGGER.error("No mets.xml file was found.");
            } else {
                LOGGER.debug("mets.xml exist.");
            }
        }
        LOGGER.debug("Check METS file took " + (System.currentTimeMillis() - start) + "ms.");
    }

    /**
     * The command check mets.xml files in the derivates.
     */
    public static void checkMets() throws Exception {
        LOGGER.debug("Check METS file start.");
        final long start = System.currentTimeMillis();
        List<String> derlist = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        for (String der : derlist) {
            checkMetsForMCRDerivateID(der);
        }
        LOGGER.debug("Check METS file took " + (System.currentTimeMillis() - start) + "ms.");
    }

    public static void removeMetsForType(String type) {
        LOGGER.debug("Remove METS file for " + type + " start.");
        final long start = System.currentTimeMillis();

        MCRQueryCondition fromcond = new MCRQueryCondition(MCRFieldDef.getDef("objectType"), "=", type);
        MCRQuery fromquery = new MCRQuery(fromcond);
        MCRResults fromresult = MCRQueryManager.search(fromquery);
        Iterator<MCRHit> fromiter = fromresult.iterator();

        while (fromiter.hasNext()) {
            MCRHit fromhit = fromiter.next();
            String fromid = fromhit.getID();

            MCRObject fromobj = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(fromid));
            MCRObjectStructure structure = fromobj.getStructure();
            List<MCRMetaLinkID> derivates = structure.getDerivates();
            for (int i = 0; i < derivates.size(); i++) {
                MCRObjectID mcrderid = derivates.get(i).getXLinkHrefID();
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

    /**
     * Remove mets.xml files from all derivates.
     * 
     * @throws Exception
     */
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

    private static void addPicturesToList(MCRDirectory dir, ArrayList<String> list) {
        for (int i = 0; i < dir.getChildren().length; i++) {
            try {
                dir = (MCRDirectory) dir.getChildren()[i];
                addPicturesToList(dir, list);
            } catch (Exception ClassCastException) {
                if (!list.contains(dir.getPath() + "/" + ((MCRFile) dir.getChildren()[i]).getName()))
                    list.add(dir.getPath() + "/" + ((MCRFile) dir.getChildren()[i]).getName());
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
            MCRMetsSave.update(MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(derivate)), filename);
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

        XPath xp = XPath.newInstance("mets:mets/mets:fileSec/mets:fileGrp[@USE='MASTER']/mets:file/mets:FLocat[@xlink:href='" + filename
                + "']");
        xp.addNamespace(MCRConstants.METS_NAMESPACE);
        xp.addNamespace(MCRConstants.XLINK_NAMESPACE);

        Object obj = xp.selectSingleNode(mets);
        if (obj != null)
            return true;

        return false;
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
