/*
 * $RCSfile: MergeProperties.java,v $
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
package org.mycore.buildtools.anttasks;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.mycore.buildtools.common.SortedProperties;

/**
 * Ant task, that can be used to merges one properties file into another.
 * New Properties will be added, existing properties overwritten.
 *  
 * @author Robert Stephan
 * 
 * @version $Revision$ $Date$
 *  
 */
public class MCRMergePropertiesTask extends Task {
	private String base, delta;

	/**
	 * sets the base properties file
	 * @param f the base properties file
	 */
	public void setBasefile(String f) {
		base = f;
	}

	/**
	 * sets the delta properties file, containing the new properties, which will
	 * be added or used as a replacement
	 * @param f the delta properties file
	 */
	public void setDeltafile(String f) {
		delta = f;
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		if (base == null || delta == null) {
			throw new BuildException("all parameter must be specified: \n"
					+ "basefile -the basic property file \n"
					+ "deltafile -the property file, containg the changes \n");
		}

		try {
		    SortedProperties baseProps = new SortedProperties();
			InputStream isBase =new FileInputStream(base); 
			baseProps.load(isBase);
			isBase.close();
			
			SortedProperties deltaProps = new SortedProperties();
			InputStream isDelta = new FileInputStream(delta);
			deltaProps.load(isDelta);
			isDelta.close();
			
			baseProps.putAll(deltaProps);
			
			baseProps.store(new FileOutputStream(base), "Merged Properties File");
		} catch (Exception e) {
			throw new BuildException("Something went wrong at reading or writing a properties file",e);
		}
	}
	
	/**
	 * simple testcase for class
	 * @param args unsed default arguments for main method
	 */
	public static void main(String[] args){
		String baseFile="C:\\workspaces\\atlibri\\antmycore\\test\\mergeproperties\\base.properties";
		String deltaFile="C:\\workspaces\\atlibri\\antmycore\\test\\mergeproperties\\delta.properties";
					
		MCRMergePropertiesTask  mx = new MCRMergePropertiesTask();
		mx.setBasefile(baseFile);
		mx.setDeltafile(deltaFile);		
		
		mx.execute();		
	}
}
