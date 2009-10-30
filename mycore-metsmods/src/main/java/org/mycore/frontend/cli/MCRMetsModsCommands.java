/*
 * 
 * $Revision: 13085 $ $Date: 2008-02-06 18:27:24 +0100 (Mi, 06 Feb 2008) $
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

package org.mycore.frontend.cli;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.common.MCRXMLTableManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.metsmods.MCRMetsModsUtil;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRResults;

/**
 * This class builds a google sitemap containing links to all documents and store them to the webapps directory. This can be configured with property variable MCR.GoogleSitemap.Directory. The web.xml file should contain a mapping to /sitemap.xml See http://www.google.com/webmasters/sitemaps/docs/en/protocol.html
 * 
 * @author Frank Luetzenkirchen
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @author Stefan Freitag (sasf)
 * @version $Revision: 13085 $ $Date: 2008-02-06 18:27:24 +0100 (Mi, 06 Feb 2008) $
 */
public final class MCRMetsModsCommands extends MCRAbstractCommands {

    /** The logger */
    private static Logger LOGGER = Logger.getLogger(MCRMetsModsCommands.class.getName());

    /**
     * The empty constructor.
     */
    public MCRMetsModsCommands() {
        super();

        MCRCommand com = null;

        com = new MCRCommand("build mets files", "org.mycore.frontend.cli.MCRMetsModsCommands.buildMets", "Create the mets.xml file in the derivate directory.");
        command.add(com);
        
        com = new MCRCommand("build mets files for type {0}","org.mycore.frontend.cli.MCRMetsModsCommands.buildMetsForType String","Create the mets.xml file in the derivate directory of given type.");
        command.add(com);
        
        com = new MCRCommand("remove mets files","org.mycore.frontend.cli.MCRMetsModsCommands.removeMets","Remove all mets files.");
        command.add(com);
        
        com = new MCRCommand("remove mets files from zoomify","org.mycore.frontend.cli.MCRMetsModsCommands.removeMetsByZoomify","Remove all mets files in zoomify derivate directorys.");
        command.add(com);
        
        com = new MCRCommand("build mets files for Derivat {0}","org.mycore.frontend.cli.MCRMetsModsCommands.buildMetsForMCRDerivateID String","Create the mets.xml file in the derivate directory of the given derivate.");
        command.add(com);
        
        com = new MCRCommand("build mets files for Object {0}","org.mycore.frontend.cli.MCRMetsModsCommands.buildMetsForMCRObjectID String","Create the mets.xml file for all dfg-derivate's of the given object.");
        command.add(com);

        com = new MCRCommand("remove mets files for type {0}","org.mycore.frontend.cli.MCRMetsModsCommands.removeMetsForType String","Remove all mets files for given type.");
        command.add(com);
        
        com = new MCRCommand("check mets files for type {0} with exclude label {1}","org.mycore.frontend.cli.MCRMetsModsCommands.checkMetsForType String String","Check mets files for given type.");
        command.add(com);
        
        com = new MCRCommand("check mets files for type {0}","org.mycore.frontend.cli.MCRMetsModsCommands.checkMetsForType String","Check mets files for given type.");
        command.add(com);
        
        com = new MCRCommand("check mets files for Object {0} with exclude label {1}","org.mycore.frontend.cli.MCRMetsModsCommands.checkMetsForMCRObjectID String String","Check mets files for given object.");
        command.add(com);
        
        com = new MCRCommand("check mets files for Object {0}","org.mycore.frontend.cli.MCRMetsModsCommands.checkMetsForMCRObjectID String","Check mets files for given object.");
        command.add(com);
       
        com = new MCRCommand("check mets files for Derivat {0}","org.mycore.frontend.cli.MCRMetsModsCommands.checkMetsForMCRDerivateID String","Check mets files for given derivat.");
        command.add(com);
        
        com = new MCRCommand("check mets files","org.mycore.frontend.cli.MCRMetsModsCommands.checkMets","Check the mets.xml file.");
        command.add(com);
    }

    
    public static final void removeMetsForType(String type) {
        LOGGER.debug("Remove METS file for "+type+" start.");
        final long start = System.currentTimeMillis();
        
        MCRQueryCondition fromcond = new MCRQueryCondition(MCRFieldDef.getDef("objectType"), "=", type);
        MCRQuery fromquery = new MCRQuery(fromcond);
        MCRResults fromresult = MCRQueryManager.search(fromquery);
        Iterator<MCRHit> fromiter = fromresult.iterator();
        
        while(fromiter.hasNext())
        {
            MCRHit fromhit = fromiter.next();
            String fromid = fromhit.getID();
                
            MCRObject fromobj = new MCRObject();
            fromobj.receiveFromDatastore(fromid);

            for(int i=0;i<fromobj.getStructure().getDerivateSize();i++)
            {
                MCRObjectID mcrderid = fromobj.getStructure().getDerivate(i).getXLinkHrefID();
                MCRDerivate mcrder = new MCRDerivate();
                MCRDirectory derdir = mcrder.receiveDirectoryFromIFS(mcrderid.toString());
                if(derdir.getChild("mets.xml")!=null)
                {
                    LOGGER.info("Removing mets.xml from Derivate "+mcrderid+" linked with Object "+fromid);
                    derdir.getChild("mets.xml").delete();
                }
                
            }
 
        }
         
        LOGGER.debug("Remove METS file for "+type+" request took " + (System.currentTimeMillis() - start) + "ms.");
    }
    
    /**
     * Remove mets.xml files from all derivates.
     * 
     * @throws Exception
     */
    public static final void removeMets() throws Exception {
        LOGGER.debug("Remove METS file start.");
        final long start = System.currentTimeMillis();
        List<String> derlist = MCRXMLTableManager.instance().listIDsOfType("derivate");
        for (String der : derlist) {
            MCRDirectory difs = MCRDirectory.getRootDirectory(der);
            if (difs != null) {
                MCRFilesystemNode mets = difs.getChild("mets.xml");
                if (mets != null) {
                    LOGGER.info("Mets file found on "+der);
                    mets.delete();
                }
            }
        }
        LOGGER.debug("Remove METS file request took " + (System.currentTimeMillis() - start) + "ms.");
    }
    
    /**
     * Remove mets.xml files from all derivates typed by zoomify.
     * 
     * @throws Exception
     */
    public static final void removeMetsByZoomify() throws Exception {
        LOGGER.debug("Remove METS file from zoomify derivates start.");
        final long start = System.currentTimeMillis();
        List<String> derlist = MCRXMLTableManager.instance().listIDsOfType("derivate");
        for (String der : derlist) {
            MCRDirectory difs = MCRDirectory.getRootDirectory(der);
            if (difs != null) {
                MCRFilesystemNode mets = difs.getChild("mets.xml");
                                              
                    MCRFilesystemNode l[] = difs.getChildren();
                    
                    boolean zipfound = false;
                    for(int i=0;i<l.length;i++)
                        if(l[i].getName().contains(".zip")) {zipfound=true;break;}
                
                if (mets != null && zipfound) {
                    LOGGER.info("Mets file found on "+der);
                    mets.delete();
                }
            }
        }
        LOGGER.debug("Remove METS file from zoomify derivates request took " + (System.currentTimeMillis() - start) + "ms.");
    }
    
    public static final void buildMetsForType(String type)
    {
        LOGGER.debug("Build METS file for type "+type+" start.");
        final long start = System.currentTimeMillis();

        MCRQueryCondition fromcond = new MCRQueryCondition(MCRFieldDef.getDef("objectType"), "=", type);
        MCRQuery fromquery = new MCRQuery(fromcond);
        MCRResults fromresult = MCRQueryManager.search(fromquery);
        Iterator<MCRHit> fromiter = fromresult.iterator();
        
        while(fromiter.hasNext())
        {
            MCRHit fromhit = fromiter.next();
            String fromid = fromhit.getID();
                
            buildMetsForMCRObjectID(fromid);
 
        }

        LOGGER.debug("Build METS file for type "+type+" request took " + (System.currentTimeMillis() - start) + "ms.");
    }
    
    public static final void checkMetsForType(String type)
    {
        checkMetsForType(type,null);   
    }
    
    public static final void checkMetsForType(String type, String exclude)
    {
        LOGGER.debug("Check METS file for type "+type+" start.");
        final long start = System.currentTimeMillis();

        MCRQueryCondition fromcond = new MCRQueryCondition(MCRFieldDef.getDef("objectType"), "=", type);
        MCRQuery fromquery = new MCRQuery(fromcond);
        MCRResults fromresult = MCRQueryManager.search(fromquery);
        Iterator<MCRHit> fromiter = fromresult.iterator();
       
        while(fromiter.hasNext())
        {
            MCRHit fromhit = fromiter.next();
            String fromid = fromhit.getID();
            
            if(exclude==null) checkMetsForMCRObjectID(fromid, null);
            else {
                StringTokenizer excluder = new StringTokenizer(exclude,",");
                while(excluder.hasMoreTokens())
                {
                    checkMetsForMCRObjectID(fromid,excluder.nextToken());
                }
                
            }
 
        }
        
        
        LOGGER.debug("Check METS file for type "+type+" request took " + (System.currentTimeMillis() - start) + "ms.");
    }
    
    public static final void checkMetsForMCRObjectID(String id)
    {
        checkMetsForMCRObjectID(id, null);
    }
        
    public static final void checkMetsForMCRObjectID(String id, String excludelabel)
    {
        LOGGER.debug("Check METS file start.");
        final long start = System.currentTimeMillis();
        
        MCRObject mcrobj = new MCRObject();
        mcrobj.receiveFromDatastore(id);
        for(int i=0;i<mcrobj.getStructure().getDerivateSize();i++)
        {
           MCRMetaLinkID mcrder = mcrobj.getStructure().getDerivate(i);
           LOGGER.debug("found derivate "+ mcrder.getXLinkTitle());
           String label = mcrder.getXLinkLabel();
           
               if(excludelabel==null) checkMetsForMCRDerivateID(mcrder.getXLinkHrefID().toString());
               else {
                   if(!label.contains(excludelabel))
                       checkMetsForMCRDerivateID(mcrder.getXLinkHrefID().toString());
               }
        }
        
        LOGGER.debug("Check METS file request took " + (System.currentTimeMillis() - start) + "ms.");
    }
    
    public static final void checkMetsForMCRDerivateID(String id)
    {
        LOGGER.debug("Check METS file start.");
        final long start = System.currentTimeMillis();
        
        MCRDirectory difs = MCRDirectory.getRootDirectory(id);
        if (difs != null) {
            MCRFilesystemNode mets = difs.getChild("mets.xml");
            if (mets == null) {
                LOGGER.info("No mets.xml file was found for "+id+".");
            }
        }
        
        LOGGER.debug("Check METS file request took " + (System.currentTimeMillis() - start) + "ms.");
    }
    
    public static final void buildMetsForMCRObjectID(String MCRID)
    {
        String baseurl = MCRConfiguration.instance().getString("MCR.baseurl", "http://127.0.0.1:8080");

        LOGGER.debug("Build METS file start.");
        final long start = System.currentTimeMillis();
        MCRObject mcrobj = new MCRObject();
        mcrobj.receiveFromDatastore(MCRID);
        for(int i=0;i<mcrobj.getStructure().getDerivateSize();i++)
        {
           MCRMetaLinkID mcrder = mcrobj.getStructure().getDerivate(i);
           LOGGER.debug("found derivate "+ mcrder.getXLinkTitle());
           String label = mcrder.getXLinkLabel();
           if(label.contains("dfg")) 
               buildMetsForMCRDerivateID(mcrder.getXLinkHrefID().toString());
        }
        LOGGER.debug("Build METS file request took " + (System.currentTimeMillis() - start) + "ms.");
    }
    
    public static final void buildMetsForMCRDerivateID(String MCRID)
    {
        String baseurl = MCRConfiguration.instance().getString("MCR.baseurl", "http://127.0.0.1:8080");

        LOGGER.debug("Build METS file start.");
        final long start = System.currentTimeMillis();
        MCRDerivate derxml = new MCRDerivate();
        derxml.receiveFromDatastore(MCRID);
        MCRObjectID docid = derxml.getDerivate().getMetaLink().getXLinkHrefID();
        MCRDirectory difs = MCRDirectory.getRootDirectory(MCRID);
        if (difs != null) {
            MCRFilesystemNode mets = difs.getChild("mets.xml");
            if (mets == null) {
                LOGGER.info("No mets.xml file was found for "+MCRID+".");
                MCRFilesystemNode[] fsnode = difs.getChildren();
                boolean checkimage = false;
                for (int i = 0; i < fsnode.length; i++) {
                    if (fsnode[i].getName().endsWith(".jpg")) {
                        checkimage = true;
                    } else {
                        checkimage = false;
                        break;
                    }
                }
                
                if (checkimage) {
                    
                    LOGGER.info("Build mets.xml for derivate "+MCRID+" with MyCoRe-Object-ID: "+docid);
                    
                    ArrayList<String> pic_list = new ArrayList<String>();
                    addPicturesToList(difs, pic_list);

                    String project = docid.getProjectId();
                    MCRConfiguration CONFIG = MCRConfiguration.instance();
                    // owner
                    String owner = CONFIG.getString("MCR.Component.MetsMods." + project + ".owner", "");
                    if (owner.trim().length() == 0) {
                        owner = CONFIG.getString("MCR.Component.MetsMods.owner", "");
                    }
                    // logo
                    String ownerLogo = CONFIG.getString("MCR.Component.MetsMods." + project + ".ownerLogo", "");
                    if (ownerLogo.trim().length() == 0) {
                        ownerLogo = CONFIG.getString("MCR.Component.MetsMods.ownerLogo", "");
                    }
                    // site url
                    String ownerSiteURL = CONFIG.getString("MCR.Component.MetsMods." + project + ".ownerSiteURL", "");
                    if (ownerSiteURL.trim().length() == 0) {
                        ownerSiteURL = CONFIG.getString("MCR.Component.MetsMods.ownerSiteURL", "");
                    }
                    // reference url
                    String referenceURL = CONFIG.getString("MCR.Component.MetsMods." + project + ".referenceURL", "");
                    if (referenceURL.trim().length() == 0) {
                        referenceURL = CONFIG.getString("MCR.Component.MetsMods.referenceURL", "");
                    }
                    // presentation url
                    String presentationURL = CONFIG.getString("MCR.Component.MetsMods." + project + ".presentationURL", "");
                    if (presentationURL.trim().length() == 0) {
                        presentationURL = CONFIG.getString("MCR.Component.MetsMods.presentationURL", "");
                    }

                    MCRMetsModsUtil mmu = new MCRMetsModsUtil();

                    Element new_mets = mmu.init_mets(MCRID);
                    Element amdSec = mmu.init_amdSec(MCRID, owner, ownerLogo, ownerSiteURL, referenceURL, presentationURL);

                    new_mets.addContent(amdSec);

                    Element mets2 = mmu.createMetsElement(pic_list, new_mets, baseurl + "servlets/MCRFileNodeServlet");

                    XMLOutputter xmlout = new XMLOutputter();
                    String full_mets = xmlout.outputString(mets2);

                    // save the builded file to IFS
                    try {
                        LOGGER.debug("storing new mets file...");
                        // startTransaction();
                        MCRFile file = new MCRFile("mets.xml", difs);
                        // commitTransaction();
                        ByteArrayInputStream bais = new ByteArrayInputStream(full_mets.getBytes());
                        long sizeDiff = file.setContentFrom(bais, false);
                       
                        file.storeContentChange(sizeDiff);
                      

                    } catch (Exception e) {
                        LOGGER.error("Error while storing new mets file...", e);
                    }
                    
                    
                }
            }
        }
        
        
        LOGGER.debug("Build METS file request took " + (System.currentTimeMillis() - start) + "ms.");
    }
    
    public static final void checkMets() throws Exception {
        
        // check time
        LOGGER.debug("Build METS file start.");
        final long start = System.currentTimeMillis();
        // get all derivates
        List<String> derlist = MCRXMLTableManager.instance().listIDsOfType("derivate");
        for (String der : derlist) {
            // get metadata MCRObjectID
            MCRDerivate derxml = new MCRDerivate();
            derxml.receiveFromDatastore(der);
            MCRObjectID docid = derxml.getDerivate().getMetaLink().getXLinkHrefID();
            // receive the IFS informations
            MCRDirectory difs = MCRDirectory.getRootDirectory(der);
            if (difs != null) {
                MCRFilesystemNode mets = difs.getChild("mets.xml");
                if (mets == null) {
                    LOGGER.info("No mets.xml file was found for "+der+".");
                }
            }
        }

        // check time
        LOGGER.debug("Build METS file request took " + (System.currentTimeMillis() - start) + "ms.");
    }

    
    /**
     * The build mets.xml files in the derivates if they does not exist and the content are images.
     */
    public static final void buildMets() throws Exception {
        
        String baseurl = MCRConfiguration.instance().getString("MCR.baseurl", "http://127.0.0.1:8080");
        
        // check time
        LOGGER.debug("Build METS file start.");
        final long start = System.currentTimeMillis();
        // get all derivates
        List<String> derlist = MCRXMLTableManager.instance().listIDsOfType("derivate");
        for (String der : derlist) {
            // get metadata MCRObjectID
            MCRDerivate derxml = new MCRDerivate();
            derxml.receiveFromDatastore(der);
            MCRObjectID docid = derxml.getDerivate().getMetaLink().getXLinkHrefID();
            // receive the IFS informations
            MCRDirectory difs = MCRDirectory.getRootDirectory(der);
            if (difs != null) {
                MCRFilesystemNode mets = difs.getChild("mets.xml");
                if (mets == null) {
                    LOGGER.info("No mets.xml file was found for "+der+".");
                    MCRFilesystemNode[] fsnode = difs.getChildren();
                    boolean checkimage = false;
                    for (int i = 0; i < fsnode.length; i++) {
                        if (fsnode[i].getName().endsWith(".jpg")) {
                            checkimage = true;
                        } else {
                            checkimage = false;
                            break;
                        }
                    }
                    if (checkimage) {
                        
                        LOGGER.info("Build mets.xml for derivate "+der+" with MyCoRe-Object-ID: "+docid);
                        
                        ArrayList<String> pic_list = new ArrayList<String>();
                        addPicturesToList(difs, pic_list);

                        String project = docid.getProjectId();
                        MCRConfiguration CONFIG = MCRConfiguration.instance();
                        // owner
                        String owner = CONFIG.getString("MCR.Component.MetsMods." + project + ".owner", "");
                        if (owner.trim().length() == 0) {
                            owner = CONFIG.getString("MCR.Component.MetsMods.owner", "");
                        }
                        // logo
                        String ownerLogo = CONFIG.getString("MCR.Component.MetsMods." + project + ".ownerLogo", "");
                        if (ownerLogo.trim().length() == 0) {
                            ownerLogo = CONFIG.getString("MCR.Component.MetsMods.ownerLogo", "");
                        }
                        // site url
                        String ownerSiteURL = CONFIG.getString("MCR.Component.MetsMods." + project + ".ownerSiteURL", "");
                        if (ownerSiteURL.trim().length() == 0) {
                            ownerSiteURL = CONFIG.getString("MCR.Component.MetsMods.ownerSiteURL", "");
                        }
                        // reference url
                        String referenceURL = CONFIG.getString("MCR.Component.MetsMods." + project + ".referenceURL", "");
                        if (referenceURL.trim().length() == 0) {
                            referenceURL = CONFIG.getString("MCR.Component.MetsMods.referenceURL", "");
                        }
                        // presentation url
                        String presentationURL = CONFIG.getString("MCR.Component.MetsMods." + project + ".presentationURL", "");
                        if (presentationURL.trim().length() == 0) {
                            presentationURL = CONFIG.getString("MCR.Component.MetsMods.presentationURL", "");
                        }

                        MCRMetsModsUtil mmu = new MCRMetsModsUtil();

                        Element new_mets = mmu.init_mets(der);
                        Element amdSec = mmu.init_amdSec(der, owner, ownerLogo, ownerSiteURL, referenceURL, presentationURL);

                        new_mets.addContent(amdSec);

                        Element mets2 = mmu.createMetsElement(pic_list, new_mets, baseurl + "servlets/MCRFileNodeServlet");

                        XMLOutputter xmlout = new XMLOutputter();
                        String full_mets = xmlout.outputString(mets2);

                        // save the builded file to IFS
                        try {
                            LOGGER.debug("storing new mets file...");
                            // startTransaction();
                            MCRFile file = new MCRFile("mets.xml", difs);
                            // commitTransaction();
                            ByteArrayInputStream bais = new ByteArrayInputStream(full_mets.getBytes());
                            long sizeDiff = file.setContentFrom(bais, false);
                           
                            file.storeContentChange(sizeDiff);
                          

                        } catch (Exception e) {
                            LOGGER.error("Error while storing new mets file...", e);
                        }
                        
                        
                    }
                }
            }
        }

        // check time
        LOGGER.debug("Build METS file request took " + (System.currentTimeMillis() - start) + "ms.");
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
}
