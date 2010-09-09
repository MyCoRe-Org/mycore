/*
 * 
 * $Revision: 1.1 $ $Date: 2009/01/30 13:04:04 $
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

package org.mycore.frontend.servlets;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.metsmods.MCRMetsModsUtil;

/**
 * This servlet generate and return a XML file including mets and mods data for processing by DFGViewer.
 * 
 * @author Stefan Freitag (sasf)
 * 
 */

public class MCRDFGViewerServlet extends MCRStartEditorServlet {
    private static final long serialVersionUID = 7125675689555945121L;

    /** The logger */
    private static Logger LOGGER = Logger.getLogger(MCRDFGViewerServlet.class.getName());

    /** The XML table API */
    static final MCRXMLMetadataManager tm = MCRXMLMetadataManager.instance();

    private static String metsfile = MCRConfiguration.instance().getString("MCR.MetsMots.MetsFile", "mets.xml");

    public void buildMetsMods(MCRServletJob job, CommonData cd) {

        // read configuration
        MCRConfiguration CONFIG = MCRConfiguration.instance();
        MCRSession session = MCRSessionMgr.getCurrentSession();
        String lang = session.getCurrentLanguage();
        String defa = CONFIG.getString("MCR.Metadata.DefaultLang", "de");
        String base = cd.myremcrid.getBase();
        String type = cd.myremcrid.getTypeId();
        // get title
        String title = CONFIG.getString("MCR.Component.MetsMods." + base + ".title", "");
        if (title.trim().length() == 0) {
            title = CONFIG.getString("MCR.Component.MetsMods." + type + ".title", "");
        }
        title = getMetadataValue(title, cd.myremcrid, lang, defa);
        // get display
        String display = CONFIG.getString("MCR.Component.MetsMods." + base + ".display", "");
        if (display.trim().length() == 0) {
            display = CONFIG.getString("MCR.Component.MetsMods." + type + ".display", "");
        }
        display = getMetadataValue(display, cd.myremcrid, lang, defa);
        // get place
        String place = CONFIG.getString("MCR.Component.MetsMods." + base + ".place", "");
        if (place.trim().length() == 0) {
            place = CONFIG.getString("MCR.Component.MetsMods." + type + ".place", "");
        }
        place = getMetadataValue(place, cd.myremcrid, lang, defa);
        // get date
        String date = CONFIG.getString("MCR.Component.MetsMods." + base + ".date", "");
        if (date.trim().length() == 0) {
            date = CONFIG.getString("MCR.Component.MetsMods." + type + ".date", "");
        }
        date = getMetadataValue(date, cd.myremcrid, lang, defa);

        // extracting mods information like title, id and date
        MCRMetsModsUtil mmu = new MCRMetsModsUtil();
        Element mods = mmu.init_mods(cd.mysemcrid.toString(), title, display, place, date);

        // now get the existing mets file

        MCRFilesystemNode node = MCRFilesystemNode.getRootNode(cd.mysemcrid.toString());
        MCRDirectory dir = (MCRDirectory) node;
        MCRFile mcrfile = (MCRFile) dir.getChild(metsfile);

        // if mets file wasn't found, generate error page and return

        if (mcrfile == null) {
            try {
                generateErrorPage(job.getRequest(), job.getResponse(), HttpServletResponse.SC_BAD_REQUEST, "No Mets file was found!",
                        new MCRException("No mets file was found!"), false);
                return;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // if mets file exist, try to read content as JDOM

        try {
            Document metsfile = mcrfile.getContentAsJDOM();

            // if tag 'dmdSec' already exist, delete the whole section
            Element dmdSec = metsfile.getRootElement().getChild("dmdSec", MCRConstants.METS_NAMESPACE);
            if (dmdSec != null)
                metsfile.getRootElement().removeChild("dmdSec", MCRConstants.METS_NAMESPACE);

            // merge the file with mods
            metsfile.getRootElement().addContent(mods);

            // try to give back the JDOM structure as servlet answer
            XMLOutputter xmlout = new XMLOutputter();
            job.getResponse().setCharacterEncoding("UTF8");
            xmlout.output(metsfile, job.getResponse().getWriter());

        } catch (MCRPersistenceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JDOMException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @SuppressWarnings("unchecked")
    private final String getMetadataValue(String conf, MCRObjectID ID, String lang, String defa) {
        if ((conf == null) || (conf.trim().length() == 0)) {
            return "";
        }
        if (!conf.startsWith("/")) {
            return conf;
        }
        LOGGER.debug("Input parameter: " + conf + "  " + lang + "  " + defa);
        try {
            LOGGER.warn("Use lang " + lang + " for get XPath element.");
            XPath xpathconf = XPath.newInstance(conf);
            Document doc = tm.retrieveXML(ID);
            List<Element> nodes = xpathconf.selectNodes(doc);
            for (Element node : nodes) {
                System.out.println(node.getName() + ":" + node.getText());
                String mylang = node.getAttributeValue("lang", org.jdom.Namespace.XML_NAMESPACE);
                if (mylang.compareTo(lang) == 0) {
                    return node.getText();
                }
            }
            for (Element node : nodes) {
                System.out.println(node.getName() + ":" + node.getText());
                String mylang = node.getAttributeValue("lang", org.jdom.Namespace.XML_NAMESPACE);
                if (mylang.compareTo(defa) == 0) {
                    return node.getText();
                }
            }
        } catch (JDOMException je) {
            return "";

        }
        return "";
    }

}
