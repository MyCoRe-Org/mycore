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
package org.mycore.frontend.cli.gui;

import javax.swing.filechooser.*;
import java.io.File;
import java.util.*;

/**
 * A new item will be added to the format-combobox in the filechooser
 * called 'XML Files' and et voila only files with extension 'xml'
 * will be shown.
 *
 * This was already written some time ago.
 *
 * @see javax.swing.filechooser.FileFilter
 *
 * @author marc schluepmann
 * @version $Revision$ $Date$
 **/
public class MCRXMLFileFilter extends FileFilter {
    /**
     * This method returns true if pathname ends with '.xml'
     *
     * @param pathname the path to examine
     * @return true if pathname ends with '.xml'
     **/
    public boolean accept( File pathname ) {
	if( pathname.isDirectory() )
	    return true;
	else {
	    StringTokenizer stok = new StringTokenizer( pathname.getName(), "." );
	    String ext = "";
	    int tokens = stok.countTokens();
	    int i = 0;
	    while( stok.hasMoreTokens() ) 
		ext = stok.nextToken();
	    return ext.equalsIgnoreCase( "xml" );
	}
    }

    public String getDescription() {
	return "XML Files";
    }
}
