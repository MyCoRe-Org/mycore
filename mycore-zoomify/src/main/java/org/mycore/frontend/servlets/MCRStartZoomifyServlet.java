/*
 * 
 * $Revision$ $Date$
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

import java.util.ArrayList;
import java.util.Properties;

import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRMetaLangText;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.metsmods.MCRMetsModsPicture;
import org.mycore.frontend.metsmods.MCRMetsModsUtil;
import org.jdom2.*;

/**
 * This servlet implements the zoomify viewer.
 * 
 * @author Stefan Freitag (sasf)
 *
 */

public class MCRStartZoomifyServlet extends MCRStartEditorServlet{

	ArrayList<String> directories = new ArrayList<String>();
	ArrayList<String> orderlabels = new ArrayList<String>();
	
	int index = -1;
	boolean init = true;

	String lastid = new String();
	
	private String getMetsFile(MCRObjectID mcrid)
	{
		MCRDirectory mcrwork = MCRDirectory.getRootDirectory(mcrid.toString());
		for(int j=0;j<mcrwork.getChildren().length;j++)
		{
			String data = mcrwork.getChildren()[j].getName();
			if(data.compareTo("mets.xml")==0)
				return mcrwork.getChildren()[j].getID();
		}
		return null;
	}
	
	public void showZoomify(MCRServletJob job, CommonData cd) throws Exception
	{
		String index = getProperty(job.getRequest(),"index");
		if(index==null) index = "0";
		if(this.index == -1) this.index = Integer.parseInt(index);
		
		String mode = getProperty(job.getRequest(),"mode");
		if(mode!=null)
		{
			if(mode.compareTo("next")==0)
			{
				if(this.index<this.directories.size()-1)
				this.index++;
			}
				
			if(mode.compareTo("prev")==0)
			{
				if(this.index>0)
				this.index--;
			}
			
			if(mode.compareTo("first")==0)
			{
			    this.index = 0;
			}
			
			if(mode.compareTo("last")==0)
			{
			    this.index = this.directories.size()-1;
			}
		}
		
		cd.myfile = pagedir + "zoomify_commit.xml";
		
		MCRDirectory mcrdir = MCRDirectory.getRootDirectory(cd.mytfmcrid.toString());
		String basedir = mcrdir.getName();
				 
		MCRObject obj = MCRMetadataManager.retrieveMCRObject(cd.myremcrid);
		MCRConfiguration CONFIG = MCRConfiguration.instance();
		String type = obj.getId().getTypeId();
		String idname = CONFIG.getString("MCR.Component.Zoomify."+type+".identifier");
		MCRMetaElement metas = obj.getMetadata().getMetadataElement(idname);
		MCRMetaLangText langtext = (MCRMetaLangText)metas.getElement(0);
		
		String mcrname = langtext.getText();
		
			if(lastid.compareTo(cd.mytfmcrid.toString())!=0)
			{
				lastid = cd.mytfmcrid.toString();
				this.index = 0;
			}
		
			//Overwrite the lists because of the update problem!
			this.directories = new ArrayList<String>();
			this.orderlabels = new ArrayList<String>();
		
			//this.init is every time true, because of disabled init parameter
		if(this.init) {

		//int numberofderivates = obj.getStructure().getDerivateSize();
		//for(int i=0;i<numberofderivates;i++)
		//{
			MCRObjectID mcrid = cd.mysemcrid;//obj.getStructure().getDerivate(i).getXLinkHrefID();
			
			if(getMetsFile(mcrid)!=null)
			{
			    Document doc = MCRFile.getFile(getMetsFile(mcrid)).getContentAsJDOM();
				ArrayList<MCRMetsModsPicture> list = MCRMetsModsUtil.getFileList(doc);
                for (MCRMetsModsPicture pic : list) {
                    this.directories.add(pic.getPicture());
                    this.orderlabels.add(pic.getOrderlabel());
                }
					
			}
			
		//}
			//this.init parameter is every time true, because of the update problem!
//			this.init = false;
		}
										
		Properties params = new Properties();
	    params.put("XSL.ImagePath", "/"+basedir+"/"+directories.get(this.index));
	    params.put("XSL.Orderlabel", orderlabels.get(this.index));
	    params.put("XSL.mcrid", cd.mytfmcrid.toString());
	    params.put("XSL.semcrid", cd.mysemcrid.toString());
	    params.put("XSL.remcrid", cd.myremcrid.toString());
	    params.put("XSL.index", String.valueOf(this.index+1));
	    params.put("XSL.max", String.valueOf(this.directories.size()));
	    params.put("XSL.label", mcrname);
	    params.put("mcrid", cd.mytfmcrid.toString());
	    params.put("semcrid", cd.mysemcrid.toString());
	    params.put("type", cd.mytype);
	    params.put("step", cd.mystep);
	    params.put("remcrid", cd.myremcrid.toString());
	    String base = getBaseURL() + cd.myfile;
	    job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));
	}
	
}
