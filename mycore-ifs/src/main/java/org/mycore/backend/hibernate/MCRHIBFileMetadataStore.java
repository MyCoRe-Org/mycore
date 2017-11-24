/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.backend.hibernate;

import java.sql.Timestamp;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.mycore.backend.hibernate.tables.MCRFSNODES;
import org.mycore.backend.hibernate.tables.MCRFSNODES_;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileMetadataManager;
import org.mycore.datamodel.ifs.MCRFileMetadataStore;
import org.mycore.datamodel.ifs.MCRFilesystemNode;

/**
 * This class implements the MCRFileMetadataStore.
 *
 */
public class MCRHIBFileMetadataStore implements MCRFileMetadataStore {
    // LOGGER
    private static Logger LOGGER = LogManager.getLogger(MCRHIBFileMetadataStore.class);

    private Session getSession() {
        return MCRHIBConnection.instance().getSession();
    }

    public MCRHIBFileMetadataStore() throws MCRPersistenceException {
    }

    @Override
    public void storeNode(MCRFilesystemNode node) throws MCRPersistenceException {

        String ID = node.getID();
        String PID = node.getParentID();
        String OWNER = node.getOwnerID();
        String NAME = node.getName();
        String LABEL = node.getLabel();
        long SIZE = node.getSize();

        GregorianCalendar DATE = node.getLastModified();

        String TYPE = null;
        String STOREID = null;
        String STORAGEID = null;
        String FCTID = null;
        String MD5 = null;

        int NUMCHDD = 0;
        int NUMCHDF = 0;
        int NUMCHTD = 0;
        int NUMCHTF = 0;

        if (node instanceof MCRFile) {
            MCRFile file = (MCRFile) node;

            TYPE = "F";
            STOREID = file.getStoreID();
            STORAGEID = file.getStorageID();
            FCTID = file.getContentTypeID();
            MD5 = file.getMD5();
        } else if (node instanceof MCRDirectory) {
            MCRDirectory dir = (MCRDirectory) node;

            TYPE = "D";
            NUMCHDD = dir.getNumChildren(MCRDirectory.DIRECTORIES, MCRDirectory.HERE);
            NUMCHDF = dir.getNumChildren(MCRDirectory.FILES, MCRDirectory.HERE);
            NUMCHTD = dir.getNumChildren(MCRDirectory.DIRECTORIES, MCRDirectory.TOTAL);
            NUMCHTF = dir.getNumChildren(MCRDirectory.FILES, MCRDirectory.TOTAL);
        } else {
            throw new MCRPersistenceException("MCRFilesystemNode must be either MCRFile or MCRDirectory");
        }

        Session session = getSession();
        MCRFSNODES fs = session.get(MCRFSNODES.class, ID);
        if (fs == null) {
            fs = new MCRFSNODES();
            fs.setId(ID);
        }
        fs.setPid(PID);
        fs.setType(TYPE);
        fs.setOwner(OWNER);
        fs.setName(NAME);
        fs.setLabel(LABEL);
        fs.setSize(SIZE);
        fs.setDate(new Timestamp(DATE.getTime().getTime()));
        fs.setStoreid(STOREID);
        fs.setStorageid(STORAGEID);
        fs.setFctid(FCTID);
        fs.setMd5(MD5);
        fs.setNumchdd(NUMCHDD);
        fs.setNumchdf(NUMCHDF);
        fs.setNumchtd(NUMCHTD);
        fs.setNumchtf(NUMCHTF);
        if (!session.contains(fs)) {
            session.saveOrUpdate(fs);
        }
    }

    @Override
    public String retrieveRootNodeID(String ownerID) throws MCRPersistenceException {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<String> rootQuery = em.createNamedQuery("MCRFSNODES.getRootID", String.class);
        rootQuery.setParameter("owner", ownerID);
        try {
            return rootQuery.getSingleResult();
        } catch (NoResultException e) {
            LOGGER.warn("There is no fsnode with OWNER = {}", ownerID);
            return null;
        }
    }

    @Override
    public MCRFilesystemNode retrieveChild(String parentID, String name) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<MCRFSNODES> childQuery = em.createNamedQuery("MCRFSNODES.getChild", MCRFSNODES.class);
        childQuery.setParameter("pid", parentID);
        childQuery.setParameter("name", name);
        try {
            MCRFSNODES node = childQuery.getSingleResult();
            return buildNode(node);
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<MCRFilesystemNode> retrieveChildren(String parentID) throws MCRPersistenceException {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<MCRFSNODES> childQuery = em.createNamedQuery("MCRFSNODES.getChildren", MCRFSNODES.class);
        childQuery.setParameter("pid", parentID);
        return childQuery.getResultList()
            .stream()
            .map(this::buildNode)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteNode(String ID) throws MCRPersistenceException {
        Session session = getSession();
        session.delete(session.get(MCRFSNODES.class, ID));
    }

    @Override
    public MCRFilesystemNode retrieveNode(String ID) throws MCRPersistenceException {
        Session session = getSession();
        MCRFSNODES node = session.get(MCRFSNODES.class, ID);
        if (node == null) {
            LOGGER.warn("There is no FSNODE with ID = {}", ID);
            return null;
        }

        return buildNode(node);
    }

    public MCRFilesystemNode buildNode(MCRFSNODES node) {
        GregorianCalendar greg = new GregorianCalendar(TimeZone.getDefault(), Locale.getDefault());
        greg.setTime(node.getDate());

        return MCRFileMetadataManager.instance().buildNode(node.getType(), node.getId(),
            node.getPid(), node.getOwner(), node.getName(), node.getLabel(), node.getSize(), greg, node.getStoreid(),
            node.getStorageid(), node.getFctid(), node.getMd5(), node.getNumchdd(), node.getNumchdf(),
            node.getNumchtd(), node.getNumchtf());
    }

    @Override
    public Iterable<String> getOwnerIDs() throws MCRPersistenceException {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<MCRFSNODES> nodes = query.from(MCRFSNODES.class);
        return em.createQuery(query
            .distinct(true)
            .select(nodes.get(MCRFSNODES_.owner)))
            .getResultList();
    }
}
