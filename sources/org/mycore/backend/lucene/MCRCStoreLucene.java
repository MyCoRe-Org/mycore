/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.backend.lucene;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintStream;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.mycore.backend.filesystem.MCRCStoreLocalFilesystem;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRInputStreamCloner;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs.MCRFileContentType;
import org.mycore.datamodel.ifs.MCRFileReader;
import org.mycore.services.plugins.TextFilterPluginManager;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * Need to insert some things here
 *
 */
public class MCRCStoreLucene extends MCRCStoreLocalFilesystem {
	private static final TextFilterPluginManager pMan=TextFilterPluginManager.getInstance();
	private static final Logger logger=Logger.getLogger(MCRCStoreLucene.class);
	private static final MCRConfiguration conf=MCRConfiguration.instance();
	
	/* (non-Javadoc)
	 * @see org.mycore.datamodel.ifs.MCRContentStore#doDeleteContent(java.lang.String)
	 */
	protected void doDeleteContent(String storageID) throws Exception {
		// TODO Auto-generated method stub
		super.doDeleteContent(storageID);
	}

	/* (non-Javadoc)
	 * @see org.mycore.datamodel.ifs.MCRContentStore#doStoreContent(org.mycore.datamodel.ifs.MCRFileReader, org.mycore.datamodel.ifs.MCRContentInputStream)
	 */
	protected String doStoreContent(
		MCRFileReader file,
		MCRContentInputStream source)
		throws Exception {
			
		MCRInputStreamCloner isc=new MCRInputStreamCloner(source);
		source=new MCRContentInputStream(isc.getNewInputStream());
		InputStream sourceStream=isc.getNewInputStream();
		String returns=super.doStoreContent(file, source);
		return returns;
	}

	/* (non-Javadoc)
	 * @see org.mycore.datamodel.ifs.MCRContentStore#init(java.lang.String)
	 */
	public void init(String storeID) {
		// TODO Auto-generated method stub
		super.init(storeID);
		pMan.loadPlugins();
	}
	
	protected Document getDocument(MCRFileReader reader,MCRContentInputStream stream){
		Document returns=new Document();
		PipedInputStream pin=new PipedInputStream();
		PipedOutputStream pout=new PipedOutputStream(pin);
		PrintStream out=new PrintStream(pout);
		BufferedReader in=new BufferedReader(new InputStreamReader(pin));
		//filter here
		
		PrintWriter ps=new PrintStream(output,true);
		Field derivateID=new Field("DerivateID","MCR_DEMO_XYZ",true,true,false);
		Field content=Field.Text("content",);
		returns.
		return null;
	}

}
