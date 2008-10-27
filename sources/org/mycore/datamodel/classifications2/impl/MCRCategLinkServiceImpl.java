/**
 * 
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRObjectReference;

/**
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date: 2008-06-30 10:08:19 +0200 (Mo, 30. Jun
 *          2008) $
 * @since 2.0
 */
public class MCRCategLinkServiceImpl implements MCRCategLinkService {

    private static Logger LOGGER = Logger.getLogger(MCRCategLinkServiceImpl.class);

    private static Class<MCRCategoryLink> LINK_CLASS = MCRCategoryLink.class;

    private static MCRCache categCache = new MCRCache(MCRConfiguration.instance().getInt("MCR.Classifications.LinkServiceImpl.CategCache.Size", 1000),
            "MCRCategLinkService category cache");

    private static MCRCategoryDAOImpl DAO = new MCRCategoryDAOImpl();

    public Map<MCRCategoryID, Number> countLinks(Collection<MCRCategoryID> categIDs) {
        return countLinksForType(categIDs, null);
    }

    @SuppressWarnings("unchecked")
    public Map<MCRCategoryID, Number> countLinksForType(Collection<MCRCategoryID> categIDs, String type) {
        boolean restrictedByType = (type != null);
        String queryName = restrictedByType ? ".NumberByTypePerCategID" : ".NumberPerCategID";
        Map<MCRCategoryID, Number> returns = new HashMap<MCRCategoryID, Number>();
        // ensure that all categIDs are grouped per classID
        Map<String, Collection<String>> perClassID = new HashMap<String, Collection<String>>();
        for (MCRCategoryID id : categIDs) {
            Collection<String> categs = perClassID.get(id.getRootID());
            if (categs == null) {
                categs = new ArrayList<String>(categIDs.size());
                perClassID.put(id.getRootID(), categs);
            }
            categs.add(id.getID());
            // initialize all categIDs with link count of zero
            returns.put(id, 0);
        }
        Session session = MCRHIBConnection.instance().getSession();
        Map<MCRCategoryID, Number> countLinks = new HashMap<MCRCategoryID, Number>();
        // for every classID do:
        for (Map.Entry<String, Collection<String>> entry : perClassID.entrySet()) {
            String classID = entry.getKey();
            Query q = session.getNamedQuery(LINK_CLASS.getName() + queryName);
            // query can take long time, please cache result
            q.setCacheable(true);
            q.setParameter("classID", classID);
            if (restrictedByType) {
                q.setParameter("type", type);
            }
            // get object count for every category (not accumulated)
            List<Object[]> result = q.list();
            for (Object[] sr : result) {
                MCRCategoryID key = new MCRCategoryID(sr[0].toString(), sr[1].toString());
                Number value = (Number) sr[2];
                countLinks.put(key, value);
                // accumulate manually due to performance problems in MySQL
                List<MCRCategory> parents = DAO.getParents(key);
                for (MCRCategory parent : parents) {
                    MCRCategoryID parentID = parent.getId();
                    Number counter = countLinks.get(parentID);
                    if (counter != null) {
                        countLinks.put(parentID, new Integer(counter.intValue() + value.intValue()));
                    } else {
                        countLinks.put(parentID, value);
                    }
                }
            }
        }
        // overwrites zero count where database returned a value
        returns.putAll(countLinks);
        return returns;
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
        q.setCacheable(true);
        q.setParameter("rootID", id.getRootID());
        q.setParameter("categID", id.getID());
        return q.list();
    }

    @SuppressWarnings("unchecked")
    public Collection<String> getLinksFromCategoryForType(MCRCategoryID id, String type) {
        Session session = MCRHIBConnection.instance().getSession();
        Query q = session.getNamedQuery(LINK_CLASS.getName() + ".ObjectIDByCategoryAndType");
        q.setCacheable(true);
        q.setParameter("rootID", id.getRootID());
        q.setParameter("categID", id.getID());
        q.setParameter("type", type);
        return q.list();
    }

    @SuppressWarnings("unchecked")
    public Collection<MCRCategoryID> getLinksFromObject(String id) {
        Session session = MCRHIBConnection.instance().getSession();
        Query q = session.getNamedQuery(LINK_CLASS.getName() + ".categoriesByObjectID");
        q.setCacheable(true);
        q.setParameter("id", id);
        List<Object[]> result = q.list();
        ArrayList<MCRCategoryID> returns = new ArrayList<MCRCategoryID>(result.size());
        for (Object[] idValues : result) {
            returns.add(new MCRCategoryID(idValues[0].toString(), idValues[1].toString()));
        }
        return returns;
    }

    public void setLinks(MCRObjectReference objectReference, Collection<MCRCategoryID> categories) {
        Session session = MCRHIBConnection.instance().getSession();
        for (MCRCategoryID categID : categories) {
            MCRCategoryLink link = new MCRCategoryLink(getMCRCategory(session, categID), objectReference);
            LOGGER.debug("Adding Link from " + link.getCategory().getId() + "(" + link.getCategory().getInternalID() + ") to " + objectReference.getObjectID());
            session.save(link);
            LOGGER.debug("===DONE: " + link.id);
        }
    }

    private static MCRCategoryImpl getMCRCategory(Session session, MCRCategoryID categID) {
        MCRCategoryImpl categ = (MCRCategoryImpl) categCache.getIfUpToDate(categID, DAO.getLastModified());
        if (categ != null)
            return categ;
        categ = MCRCategoryDAOImpl.getByNaturalID(session, categID);
        if (categ == null) {
            throw new MCRException("Category " + categID + " does not exists");
        }
        categCache.put(categID, categ);
        return categ;
    }

    @SuppressWarnings("unchecked")
    public boolean hasLinks(MCRCategoryID categID) {
        Session session = MCRHIBConnection.instance().getSession();
        LOGGER.debug("first fetch all internalID of all category under " + categID);
        Criteria classCriteria = session.createCriteria(MCRCategoryImpl.class);
        classCriteria.setProjection(Projections.property("internalID"));
        classCriteria.add(Restrictions.eq("rootID", categID.getRootID()));
        if (!categID.isRootID()) {
            MCRCategoryImpl category = MCRCategoryDAOImpl.getByNaturalID(session, categID);
            if (category == null) {
                LOGGER.warn("Category does not exist: " + categID);
                return false;
            }
            classCriteria.add(Restrictions.between("left", category.getLeft(), category.getRight()));
        }
        List<Number> internalIDs = classCriteria.list();
        if (internalIDs.size() == 0) {
            LOGGER.warn("Category does not exist: " + categID);
            return false;
        }
        LOGGER.debug("check if a single linked category is part of " + categID);
        Query linkQuery = session.getNamedQuery(LINK_CLASS.getName() + ".hasLinks");
        linkQuery.setParameterList("internalIDs", internalIDs);
        return linkQuery.iterate().hasNext();
    }
}
