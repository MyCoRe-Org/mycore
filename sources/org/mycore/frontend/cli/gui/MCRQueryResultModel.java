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
package mycore.gui;

import javax.swing.*;
import javax.swing.event.*;
import mycore.datamodel.*;
import mycore.xml.*;

/**
 * This class implements the ListModel interface for displaying MyCoRe
 * object ids in JList.
 *
 * @author marc schluepmann
 * @version $Revision$ $Date$
 */
public class MCRQueryResultModel implements ListModel { 
    private MCRQueryResultArray results;

    public MCRQueryResultModel( MCRQueryResultArray result ) {
	results = result;
    }

    public Object getElementAt( int index ) {
	MCRObjectIdentifier res = new MCRObjectIdentifier( results.getId( index ), 
							   results.getHost( index ) 
							   );
	return res;
    }
    
    public int getSize() {
	return results.size();
    }

    public void addListDataListener( ListDataListener l ) {}

    public void removeListDataListener( ListDataListener l ) {}
}
