/*
 * $RCSfile: MCRExecuteCommandTask.java,v $
 * $Revision: 1.2 $ $Date: 2006/11/22 10:50:21 $
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.mycore.frontend.cli.MCRCommandLineInterface;

/**
 * This class is an ant tasks to call the MyCoRe CLI in build process
 * 
 * @author Robert Stephan
 * 
 * @version $Revision: 13085 $ $Date: 2008-02-06 18:27:24 +0100 (Mi, 06 Feb 2008) $
 * 
 */
public class MCRExecuteCommandTask extends Task {
	String commands;
	String basedir = null;

	/**
	 * method used, to read the body of an ant task xml element
	 * @param commands
	 */
	public void addText(String commands) {
		this.commands = commands;
	}

	/**
	 * set the base directory, that should be used while executing the commands
	 * @param basedir - the directory
	 */			
	public void setBasedir(String basedir) {
		this.basedir = basedir;
	}
	
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		System.out.println(commands);
		BufferedReader reader = new BufferedReader(new StringReader(commands));
		String line;
		List<String> list = new ArrayList<String>();
		try {
			while ((line = reader.readLine()) != null) {
				line = line.trim();

				if (line.startsWith("#") || (line.length() == 0)) {
					continue;
				}
				list.add(line);
			}
		} catch (IOException e) {
			// do nothing
		}
		System.out.println(list.size());
		StringBuffer sbCommands = new StringBuffer();
		for (String s : list) {
			sbCommands.append(s).append(";;");
		}

		MCRCommandLineInterface.main(new String[] { sbCommands.toString() });
	}
}
