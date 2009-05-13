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

import java.awt.Font;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRMetaHistoryDate;
import org.mycore.datamodel.metadata.MCRMetaLangText;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.frontend.metsmods.MetsModsUtil;


/**
 * This servlet generate and return a XML file including mets and mods data for processing by DFGViewer.
 * 
 * @author Stefan Freitag (sasf)
 *
 */

public class MCRDFGViewerServlet extends MCRStartEditorServlet{
	
	private static String metsfile = CONFIG.getString("MCR.MetsMots.ConfigFile", "mets.xml");
	
	public void buildMetsMods(MCRServletJob job, CommonData cd)
	{
		//load MCRObject via MyCoRe-ID
		MCRObject mcrobj = new MCRObject();
		mcrobj.receiveFromDatastore(cd.myremcrid.getId());
		
		//extracting mods information like title, id and date
		MetsModsUtil mmu = new MetsModsUtil();
		
		MCRMetaElement metaelm = mcrobj.getMetadataElement("source01s");
		String doc_title = ((MCRMetaLangText)metaelm.getElement(0)).getText();
		
        metaelm = mcrobj.getMetadataElement("source12s");
        MCRMetaHistoryDate mcrdate = (MCRMetaHistoryDate)metaelm.getElement(0); 
        String doc_date = mcrdate.getText();
        
        Element mods = mmu.init_mods(cd.mysemcrid.getId(), doc_title, "Quelle", "siehe Signatur", doc_date);
        
        //now get the existing mets file
        
        MCRFilesystemNode node = MCRFilesystemNode.getRootNode(cd.mysemcrid.getId());
        MCRDirectory dir = (MCRDirectory)node;
        MCRFile mcrfile = (MCRFile)dir.getChild(metsfile);
        
        //if mets file wasn't found, generate error page and return
        
        if(mcrfile==null) {
        	try {
				generateErrorPage(job.getRequest(), job.getResponse(), HttpServletResponse.SC_BAD_REQUEST, "No Mets file was found!", new MCRException("No mets file was found!"), false);
				return;
        	} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        //if mets file exist, try to read content as JDOM
        
        try {
			Document metsfile = mcrfile.getContentAsJDOM();
			
			//if tag 'dmdSec' already exist, delete the whole section
			Element dmdSec = metsfile.getRootElement().getChild("dmdSec",MCRConstants.METS_NAMESPACE);
			if(dmdSec!=null)
				metsfile.getRootElement().removeChild("dmdSec",MCRConstants.METS_NAMESPACE);
			
			//merge the file with mods
			metsfile.getRootElement().addContent(mods);
			
			//try to give back the JDOM structure as servlet answer
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

}
