/**
 * $RCSfile: MCROAIQueryService.java,v $
 * $Revision: 1.5 $ $Date: 2003/01/28 13:30:25 $
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

package org.mycore.services.oai;

import org.mycore.datamodel.classifications.MCRCategoryItem;
import org.mycore.datamodel.classifications.MCRClassificationItem;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.services.oai.MCROAIQuery;

/**
 * @author Werner Gresshoff
 *
 * @version $Revision: 1.5 $ $Date: 2003/01/28 13:30:25 $
 *
 * This is the MyCoRe-Implementation of the <i>MCROAIQuery</i>-Interface.
 */
public class MCROAIQueryService implements MCROAIQuery {

	/**
	 * Method exists. Checks if the given ID exists in the data repository
	 * @param id The ID to be checked
	 * @return boolean
	 */
	public boolean exists(String id) {
		return MCRObject.existInDatastore(id);
	}
	
	/**
	 * Method listSets. Gets a list of classificationId's and Labels for a given ID
	 * @param classificationId
	 * @return List A list that contains an array of three Strings: the category id, 
	 * 				the label and a description
	 */
	public List listSets(String classificationId) {
		List list = new ArrayList();
        MCRClassificationItem repository = MCRClassificationItem.
            getClassificationItem(classificationIdentifier);
        if ((repository != null) && repository.hasChildren()) {
        	MCRCategoryItem[] children = repository.getChildren();

	        for (int i = 0; i < children.length; i++) {
            	String[] set = new String[3];
    	        set[0] = new String(children[i].getID());
        	    set[1] = new String(children[i].getLabel("en"));
            	set[2] = new String(children[i].getDescription("en"));

        	    if (children[i].hasChildren()) {
/*            	    document = getSets(document, ns, children[i].getChildren(),
                	    parentSpec + categoryID + ":");
*/	            }
			}
			
			return list;
        }
        return null;
	}
}
