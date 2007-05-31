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
package org.mycore.datamodel.classifications2.impl;

import java.util.Collection;
import java.util.Map;

import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRObjectReference;

/**
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 2.0
 */
public class MCRCategLinkServiceImpl implements MCRCategLinkService {

    public Map<MCRCategoryID, Number> countLinks(Collection<MCRCategoryID> categIDs) {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<MCRCategoryID, Number> countLinksForType(Collection<MCRCategoryID> categIDs, String type) {
        // TODO Auto-generated method stub
        return null;
    }

    public void deleteLink(String id) {
        // TODO Auto-generated method stub

    }

    public void deleteLinks(Collection<String> ids) {
        // TODO Auto-generated method stub

    }

    public Collection<String> getLinksFromCategory(MCRCategoryID id) {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<String> getLinksFromCategoryForType(MCRCategoryID id, String type) {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<MCRCategoryID> getLinksFromObject(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    public void setLinks(Map<MCRObjectReference, MCRCategoryID> map) {
        // TODO Auto-generated method stub

    }

    public void beginTransaction() {
        // TODO Auto-generated method stub

    }

    public void commitTransaction() {
        // TODO Auto-generated method stub

    }

    public void rollBackTransaction() {
        // TODO Auto-generated method stub

    }

}
