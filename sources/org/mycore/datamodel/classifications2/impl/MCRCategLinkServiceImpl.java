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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRObjectReference;

/**
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 2.0
 */
public class MCRCategLinkServiceImpl implements MCRCategLinkService {

    static MCRCategoryDAO DAO = new MCRCategoryDAOImpl();

    private static Logger LOGGER = Logger.getLogger(MCRCategLinkServiceImpl.class);

    private static Class<MCRCategoryLink> LINK_CLASS = MCRCategoryLink.class;

    @SuppressWarnings("unchecked")
    public Map<MCRCategoryID, Number> countLinks(Collection<MCRCategoryID> categIDs) {
        Session session = MCRHIBConnection.instance().getSession();
        Map<MCRCategoryID, Number> countLinks = new HashMap<MCRCategoryID, Number>();
        Query q = session.getNamedQuery(LINK_CLASS.getName() + ".NumberPerCategID");
        q.setParameterList("categIDs", categIDs);
        List<Object[]> result = q.list();
        for (Object[] sr : result) {
            MCRCategoryID key = (MCRCategoryID) sr[0];
            Number value = (Number) sr[1];
            countLinks.put(key, value);
        }
        return countLinks;
    }

    @SuppressWarnings("unchecked")
    public Map<MCRCategoryID, Number> countLinksForType(Collection<MCRCategoryID> categIDs, String type) {
        Session session = MCRHIBConnection.instance().getSession();
        Map<MCRCategoryID, Number> countLinks = new HashMap<MCRCategoryID, Number>();
        Query q = session.getNamedQuery(LINK_CLASS.getName() + ".NumberByTypePerCategID");
        q.setParameterList("categIDs", categIDs);
        q.setParameter("type", type);
        List<Object[]> result = q.list();
        for (Object[] sr : result) {
            MCRCategoryID key = (MCRCategoryID) sr[0];
            Number value = (Number) sr[1];
            countLinks.put(key, value);
        }
        return countLinks;
    }

    public void deleteLink(String id) {
        Session session = MCRHIBConnection.instance().getSession();
        Query q = session.getNamedQuery(LINK_CLASS.getName() + ".deleteByObjectID");
        q.setParameter("id", id);
        int deleted = q.executeUpdate();
        LOGGER.debug("Number of Links deleted: " + deleted);
    }

    public void deleteLinks(Collection<String> ids) {
        Session session = MCRHIBConnection.instance().getSession();
        Query q = session.getNamedQuery(LINK_CLASS.getName() + ".deleteByObjectCollection");
        q.setParameterList("ids", ids);
        int deleted = q.executeUpdate();
        LOGGER.debug("Number of Links deleted: " + deleted);
    }

    @SuppressWarnings("unchecked")
    public Collection<String> getLinksFromCategory(MCRCategoryID id) {
        Session session = MCRHIBConnection.instance().getSession();
        Query q = session.getNamedQuery(LINK_CLASS.getName() + ".ObjectIDByCategory");
        q.setParameter("category", id);
        return q.list();
    }

    @SuppressWarnings("unchecked")
    public Collection<String> getLinksFromCategoryForType(MCRCategoryID id, String type) {
        Session session = MCRHIBConnection.instance().getSession();
        Query q = session.getNamedQuery(LINK_CLASS.getName() + ".ObjectIDByCategoryAndType");
        q.setParameter("category", id);
        q.setParameter("type", id);
        return q.list();
    }

    @SuppressWarnings("unchecked")
    public Collection<MCRCategoryID> getLinksFromObject(String id) {
        Session session = MCRHIBConnection.instance().getSession();
        Query q = session.getNamedQuery(LINK_CLASS.getName() + ".categoriesByObjectID");
        q.setParameter("id", id);
        return q.list();
    }

    public void setLinks(MCRObjectReference objectReference, Collection<MCRCategoryID> categories) {
        Session session = MCRHIBConnection.instance().getSession();
        for (MCRCategoryID categID : categories) {
            MCRCategoryLink link = new MCRCategoryLink(MCRCategoryDAOImpl.getByNaturalID(session, categID), objectReference);
            LOGGER.debug("Adding Link from " + link.getCategory().getId() + "(" + link.getCategory().getInternalID() + ") to " + objectReference.getObjectID());
            session.save(link);
            LOGGER.debug("===DONE: " + link.id);
        }
    }
}
