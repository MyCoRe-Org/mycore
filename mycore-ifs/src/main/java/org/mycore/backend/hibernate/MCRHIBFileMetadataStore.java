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

    public MCRHIBFileMetadataStore() throws MCRPersistenceException {
    }

    @Override
    public void storeNode(MCRFilesystemNode node) throws MCRPersistenceException {

        String id = node.getID();
        String pid = node.getParentID();
        String owner = node.getOwnerID();
        String name = node.getName();
        String label = node.getLabel();
        long size = node.getSize();

        GregorianCalendar date = node.getLastModified();

        String type = null;
        String storeid = null;
        String storageid = null;
        String fctid = null;
        String md5 = null;

        int numchdd = 0;
        int numchdf = 0;
        int numchtd = 0;
        int numchtf = 0;

        if (node instanceof MCRFile) {
            MCRFile file = (MCRFile) node;

            type = "F";
            storeid = file.getStoreID();
            storageid = file.getStorageID();
            fctid = file.getContentTypeID();
            md5 = file.getMD5();
        } else if (node instanceof MCRDirectory) {
            MCRDirectory dir = (MCRDirectory) node;

            type = "D";
            numchdd = dir.getNumChildren(MCRDirectory.DIRECTORIES, MCRDirectory.HERE);
            numchdf = dir.getNumChildren(MCRDirectory.FILES, MCRDirectory.HERE);
            numchtd = dir.getNumChildren(MCRDirectory.DIRECTORIES, MCRDirectory.TOTAL);
            numchtf = dir.getNumChildren(MCRDirectory.FILES, MCRDirectory.TOTAL);
        } else {
            throw new MCRPersistenceException("MCRFilesystemNode must be either MCRFile or MCRDirectory");
        }

        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        MCRFSNODES fs = em.find(MCRFSNODES.class, id);
        if (fs == null) {
            fs = new MCRFSNODES();
            fs.setId(id);
        }
        fs.setPid(pid);
        fs.setType(type);
        fs.setOwner(owner);
        fs.setName(name);
        fs.setLabel(label);
        fs.setSize(size);
        fs.setDate(new Timestamp(date.getTime().getTime()));
        fs.setStoreid(storeid);
        fs.setStorageid(storageid);
        fs.setFctid(fctid);
        fs.setMd5(md5);
        fs.setNumchdd(numchdd);
        fs.setNumchdf(numchdf);
        fs.setNumchtd(numchtd);
        fs.setNumchtf(numchtf);
        if (!em.contains(fs)) {
            em.persist(fs);
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
    public void deleteNode(String id) throws MCRPersistenceException {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        MCRFSNODES node = em.find(MCRFSNODES.class, id);
        if (node != null) {
            em.remove(node); //MCR-1634
        }
    }

    @Override
    public MCRFilesystemNode retrieveNode(String id) throws MCRPersistenceException {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        MCRFSNODES node = em.find(MCRFSNODES.class, id);
        if (node == null) {
            LOGGER.warn("There is no FSNODE with ID = {}", id);
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
