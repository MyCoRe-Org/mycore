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
/*  											*/
/* Image Viewer - MCR-IView 1.0, 05-2006  		*/
/* +++++++++++++++++++++++++++++++++++++		*/
/*  											*/
/* Andreas Trappe 	- concept, devel. in misc.  */
/* Britta Kapitzki	- Design					*/
/* Thomas Scheffler - html prototype		    */
/* Stephan Schmidt 	- html prototype			*/
/*  											*/

package org.mycore.frontend.iview;

import java.util.Comparator;

import org.mycore.common.MCRException;
import org.mycore.common.MCRUsageException;
import org.mycore.datamodel.ifs.MCRFilesystemNode;

/**
 * @author Thomas Scheffler (yagee), Andreas Trappe
 *
 * @version $Revision$ $Date$
 */
public class MCRFileNodeComparator implements Comparator {
	
	public static final int NAME=1;
	public static final int SIZE=2;
	public static final int LAST_MODIFIED=3;
	private int sort;
	public static final int ASCENDING=4;
	public static final int DESCENDING=5;	
	private int order;
	/**
	 * 
	 */
	public MCRFileNodeComparator(int criteria, int sortOrder) {
		super();
		sort=criteria;
		order=sortOrder;
	}

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Object arg0, Object arg1) {
		if (!((arg0 instanceof MCRFilesystemNode) && (arg1 instanceof MCRFilesystemNode))){
			throw new MCRUsageException("MCRUsageException occured in MCRFileNodeComparator");
		}
		MCRFilesystemNode node1,node2;
		node1=(MCRFilesystemNode)arg0;
		node2=(MCRFilesystemNode)arg1;
		switch (sort){
		case NAME: {
			if (order==4) return compareName(node1, node2);
			else return compareName(node2,node1);
		}
		case SIZE: {
			if (order==4) return compareSize(node2, node1);
			else return compareSize(node1, node2);
		}
		case LAST_MODIFIED: {
			if (order==4) return compareLastModified(node1, node2);
			else return compareLastModified(node2, node1);
		}
		default: throw new MCRException("submitted sort criteria is unknown");
		}
	}
	
	public int compareSize(MCRFilesystemNode node1, MCRFilesystemNode node2){
		return (int)((node2.getSize()-node1.getSize())/1024);
		
	}
	public int compareName(MCRFilesystemNode node1, MCRFilesystemNode node2){
		return node1.getName().compareTo(node2.getName());
		
	}
	public int compareLastModified(MCRFilesystemNode node1, MCRFilesystemNode node2){
		return node1.getLastModified().getTime().compareTo(node2.getLastModified().getTime());
	}
}
