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
import java.util.HashSet;
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
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategory;
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

    private static MCRHIBConnection HIB_CONNECTION_INSTANCE;

    private static Logger LOGGER = Logger.getLogger(MCRCategLinkServiceImpl.class);

    private static Class<MCRCategoryLink> LINK_CLASS = MCRCategoryLink.class;

    private static MCRCache categCache = new MCRCache(MCRConfiguration.instance().getInt(
            "MCR.Classifications.LinkServiceImpl.CategCache.Size", 1000), "MCRCategLinkService category cache");

    private static MCRCategoryDAOImpl DAO = new MCRCategoryDAOImpl();

    public MCRCategLinkServiceImpl() {
        HIB_CONNECTION_INSTANCE = MCRHIBConnection.instance();
    }

    public Map<MCRCategoryID, Number> countLinks(MCRCategory parent, boolean childrenOnly) {
        return countLinksForType(parent, null, childrenOnly);
    }

    public Map<MCRCategoryID, Number> countLinksForType(MCRCategory parent, String type, boolean childrenOnly) {
        boolean restrictedByType = (type != null);
        String queryName;
        if (childrenOnly) {
            queryName = restrictedByType ? ".NumberByTypePerChildOfParentID" : ".NumberPerChildOfParentID";
        } else {
            queryName = restrictedByType ? ".NumberByTypePerClassID" : ".NumberPerClassID";
        }
        Map<MCRCategoryID, Number> countLinks = new HashMap<MCRCategoryID, Number>();
        Collection<MCRCategoryID> ids = childrenOnly ? getAllChildIDs(parent) : getAllCategIDs(parent);
        for (MCRCategoryID id : ids) {
            // initialize all categIDs with link count of zero
            countLinks.put(id, 0);
        }
        Session session = HIB_CONNECTION_INSTANCE.getSession();
        //have to use rootID here if childrenOnly=false
        //old classification browser/editor could not determine links correctly otherwise 
        final MCRCategoryID fetchID = childrenOnly ? parent.getId() : parent.getRoot().getId();
        parent = getMCRCategory(session, fetchID);
        if (parent == null) {
            LOGGER.warn("Could not load category: " + fetchID);
            return countLinks;
        }
        LOGGER.info("parentID:" + fetchID);
        String classID = fetchID.getRootID();
        Query q = HIB_CONNECTION_INSTANCE.getNamedQuery(LINK_CLASS.getName() + queryName);
        // query can take long time, please cache result
        q.setCacheable(true);
        q.setParameter("classID", classID);
        if (childrenOnly) {
            q.setParameter("parentID", ((MCRCategoryImpl) parent).getInternalID());
        }
        if (restrictedByType) {
            q.setParameter("type", type);
        }
        // get object count for every category (not accumulated)
        @SuppressWarnings("unchecked")
        List<Object[]> result = q.list();
        for (Object[] sr : result) {
            MCRCategoryID key = new MCRCategoryID(classID, sr[0].toString());
            Number value = (Number) sr[1];
            countLinks.put(key, value);
        }
        return countLinks;
    }

    public void deleteLink(String id) {
        Query q = HIB_CONNECTION_INSTANCE.getNamedQuery(LINK_CLASS.getName() + ".deleteByObjectID");
        q.setParameter("id", id);
        int deleted = q.executeUpdate();
        LOGGER.debug("Number of Links deleted: " + deleted);
    }

    public void deleteLinks(Collection<String> ids) {
        Query q = HIB_CONNECTION_INSTANCE.getNamedQuery(LINK_CLASS.getName() + ".deleteByObjectCollection");
        q.setParameterList("ids", ids);
        int deleted = q.executeUpdate();
        LOGGER.debug("Number of Links deleted: " + deleted);
    }

    @SuppressWarnings("unchecked")
    public Collection<String> getLinksFromCategory(MCRCategoryID id) {
        Query q = HIB_CONNECTION_INSTANCE.getNamedQuery(LINK_CLASS.getName() + ".ObjectIDByCategory");
        q.setCacheable(true);
        q.setParameter("rootID", id.getRootID());
        q.setParameter("categID", id.getID());
        return q.list();
    }

    @SuppressWarnings("unchecked")
    public Collection<String> getLinksFromCategoryForType(MCRCategoryID id, String type) {
        Query q = HIB_CONNECTION_INSTANCE.getNamedQuery(LINK_CLASS.getName() + ".ObjectIDByCategoryAndType");
        q.setCacheable(true);
        q.setParameter("rootID", id.getRootID());
        q.setParameter("categID", id.getID());
        q.setParameter("type", type);
        return q.list();
    }

    @SuppressWarnings("unchecked")
    public Collection<MCRCategoryID> getLinksFromObject(String id) {
        Query q = HIB_CONNECTION_INSTANCE.getNamedQuery(LINK_CLASS.getName() + ".categoriesByObjectID");
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
        Session session = HIB_CONNECTION_INSTANCE.getSession();
        for (MCRCategoryID categID : categories) {
            MCRCategoryLink link = new MCRCategoryLink(getMCRCategory(session, categID), objectReference);
            LOGGER.debug("Adding Link from " + link.getCategory().getId() + "(" + link.getCategory().getInternalID() + ") to "
                    + objectReference.getObjectID());
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
            return null;
        }
        categCache.put(categID, categ);
        return categ;
    }

    @SuppressWarnings("unchecked")
    public Map<MCRCategoryID, Boolean> hasLinks(MCRCategory category) {
        //a rather simple implementation
        boolean useSingleDBQuery = MCRConfiguration.instance()
                .getBoolean("MCR.Classifications.LinkServiceImpl.HasLinks.SingleQuery", false);
        Map<MCRCategoryID, Number> countMap = useSingleDBQuery ? countLinks(category, false) : null;
        final Collection<MCRCategoryID> allCategIDs = getAllCategIDs(category);
        HashMap<MCRCategoryID, Boolean> boolMap = new HashMap<MCRCategoryID, Boolean>(allCategIDs.size());
        Session session = HIB_CONNECTION_INSTANCE.getSession();
        for (MCRCategoryID categID : allCategIDs) {
            if (useSingleDBQuery) {
                boolMap.put(categID, countMap.get(categID).intValue() > 0 ? Boolean.TRUE : Boolean.FALSE);
            } else {
                LOGGER.debug("first fetch all internalID of all category under " + categID);
                Criteria classCriteria = session.createCriteria(MCRCategoryImpl.class);
                classCriteria.setProjection(Projections.property("internalID"));
                classCriteria.add(Restrictions.eq("rootID", categID.getRootID()));
                if (!categID.isRootID()) {
                    MCRCategoryImpl categoryImpl = MCRCategoryDAOImpl.getByNaturalID(session, categID);
                    if (categoryImpl == null) {
                        LOGGER.warn("Category does not exist: " + categID);
                        boolMap.put(categID, false);
                        continue;
                    }
                    classCriteria.add(Restrictions.between("left", categoryImpl.getLeft(), categoryImpl.getRight()));
                }
                List<Number> internalIDs = classCriteria.list();
                if (internalIDs.size() == 0) {
                    LOGGER.warn("Category does not exist: " + categID);
                    boolMap.put(categID, false);
                    continue;
                }
                LOGGER.debug("check if a single linked category is part of " + categID);
                Query linkQuery = HIB_CONNECTION_INSTANCE.getNamedQuery(LINK_CLASS.getName() + ".hasLinks");
                /*
                 * do query in chunks of 5000 internalIDs
                 * this prevents StackOverflowErrors in hibernate 
                 */
                int size = internalIDs.size();
                int maxSize = 5000;
                LOGGER.debug("internalIDs size:" + size);
                for (int i = 0; i < Math.ceil(size / (double) maxSize); i++) {
                    int begin = i * maxSize;
                    int end = i * maxSize + maxSize;
                    if (end >= size)
                        end = size;
                    List<Number> idParam = internalIDs.subList(begin, end);
                    linkQuery.setParameterList("internalIDs", idParam);
                    linkQuery.setMaxResults(1);
                    boolMap.put(categID, linkQuery.iterate().hasNext());
                }
            }
        }
        return boolMap;
    }

    private static Collection<MCRCategoryID> getAllCategIDs(MCRCategory category) {
        HashSet<MCRCategoryID> ids = new HashSet<MCRCategoryID>();
        ids.add(category.getId());
        for (MCRCategory cat : category.getChildren()) {
            ids.addAll(getAllCategIDs(cat));
        }
        return ids;
    }

    private static Collection<MCRCategoryID> getAllChildIDs(MCRCategory category) {
        HashSet<MCRCategoryID> ids = new HashSet<MCRCategoryID>();
        for (MCRCategory cat : category.getChildren()) {
            ids.add(cat.getId());
        }
        return ids;
    }
}
