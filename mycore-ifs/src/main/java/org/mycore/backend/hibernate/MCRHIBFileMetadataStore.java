/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
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
        MCRFSNODES fs = (MCRFSNODES) session.get(MCRFSNODES.class, ID);
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

    public String retrieveRootNodeID(String ownerID) throws MCRPersistenceException {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<MCRFSNODES> nodes = query.from(MCRFSNODES.class);
        try {
            return em.createQuery(query
                .select(nodes.get(MCRFSNODES_.id))
                .where(
                    cb.isNull(nodes.get(MCRFSNODES_.pid)),
                    cb.equal(nodes.get(MCRFSNODES_.owner), ownerID)))
                .getSingleResult();
        } catch (NoResultException e) {
            LOGGER.warn("There is no fsnode with OWNER = " + ownerID);
            return null;
        }
    }

    public MCRFilesystemNode retrieveChild(String parentID, String name) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRFSNODES> query = cb.createQuery(MCRFSNODES.class);
        Root<MCRFSNODES> nodes = query.from(MCRFSNODES.class);
        try {
            MCRFSNODES node = em.createQuery(query
                .where(
                    cb.equal(nodes.get(MCRFSNODES_.pid), parentID),
                    cb.equal(nodes.get(MCRFSNODES_.name), name)))
                .getSingleResult();
            return buildNode(node);
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<MCRFilesystemNode> retrieveChildren(String parentID) throws MCRPersistenceException {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRFSNODES> query = cb.createQuery(MCRFSNODES.class);
        Root<MCRFSNODES> nodes = query.from(MCRFSNODES.class);
        return em.createQuery(query
            .where(
                cb.equal(nodes.get(MCRFSNODES_.pid), parentID)))
            .getResultList()
            .stream()
            .map(this::buildNode)
            .collect(Collectors.toList());
    }

    public void deleteNode(String ID) throws MCRPersistenceException {
        Session session = getSession();
        session.delete(session.get(MCRFSNODES.class, ID));
    }

    public MCRFilesystemNode retrieveNode(String ID) throws MCRPersistenceException {
        Session session = getSession();
        MCRFSNODES node = (MCRFSNODES) session.get(MCRFSNODES.class, ID);
        if (node == null) {
            LOGGER.warn("There is no FSNODE with ID = " + ID);
            return null;
        }

        return buildNode(node);
    }

    public MCRFilesystemNode buildNode(MCRFSNODES node) {
        GregorianCalendar greg = new GregorianCalendar(TimeZone.getDefault(), Locale.getDefault());
        greg.setTime(node.getDate());

        MCRFilesystemNode filesystemNode = MCRFileMetadataManager.instance().buildNode(node.getType(), node.getId(),
            node.getPid(), node.getOwner(), node.getName(), node.getLabel(), node.getSize(), greg, node.getStoreid(),
            node.getStorageid(), node.getFctid(), node.getMd5(), node.getNumchdd(), node.getNumchdf(),
            node.getNumchtd(), node.getNumchtf());
        return filesystemNode;
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
