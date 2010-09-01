package org.mycore.datamodel;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRXMLTABLE;
import org.mycore.datamodel.common.MCRXMLTableManager;
import org.mycore.datamodel.ifs2.MCRVersionedMetadata;
import org.mycore.datamodel.ifs2.MCRVersioningMetadataStore;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * allows online migration of xml metadata from SQL database to IFS2 store.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRMigratingXMLStore extends MCRVersioningMetadataStore {
    private static Logger LOGGER = Logger.getLogger(MCRMigratingXMLStore.class);

    @Override
    public MCRVersionedMetadata retrieve(int id) throws IOException {
        MCRVersionedMetadata versionedMetadata = super.retrieve(id);
        if (versionedMetadata == null || versionedMetadata.isDeleted()) {
            migrateObject(id);
            versionedMetadata = super.retrieve(id);
        }
        return versionedMetadata;
    }

    @Override
    public boolean exists(int id) throws IOException {
        if (super.exists(id))
            return true;
        migrateObject(id);
        return super.exists(id);
    }

    @Override
    public synchronized int getHighestStoredID() {
        String[] base = getID().split("_");
        return Math.max(getHighestStoredID(base[0], base[1]), super.getHighestStoredID());
    }

    @Override
    public Iterator<Integer> listIDs(boolean order) {
        String[] base = getID().split("_");
        TreeSet<Integer> set = new TreeSet<Integer>();
        Session session = MCRHIBConnection.instance().getSession();
        @SuppressWarnings("unchecked")
        List<String> l = session
            .createQuery(
                "select distinct(key.id) from org.mycore.backend.hibernate.tables.MCRXMLTABLE where MCRID like '" + base[0] + "#_" + base[1]
                    + "#_%' ESCAPE '#'")
            .list();
        for (String id : l) {
            int intid = Integer.parseInt(id.substring(id.lastIndexOf('_') + 1));
            set.add(intid);
        }
        Iterator<Integer> listIDs = super.listIDs(order);
        while (listIDs.hasNext()) {
            set.add(listIDs.next());
        }
        return order ? set.iterator() : set.descendingIterator();
    }

    private final synchronized int getHighestStoredID(String project, String type) {
        Session session = MCRHIBConnection.instance().getSession();
        List<?> l = session.createQuery(
            "select max(key.id) from org.mycore.backend.hibernate.tables.MCRXMLTABLE where MCRID like '" + project + "#_" + type + "#_%' ESCAPE '#'").list();
        if (l.size() == 0 || l.get(0) == null)
            return 0;
        else {
            String last = (String) (l.get(0));
            return Integer.parseInt(last.substring(last.lastIndexOf('_') + 1));
        }
    }

    private void migrateObject(int id) throws IOException {
        MCRObjectID mcrId = new MCRObjectID(getID() + "_" + id);
        Session session = MCRHIBConnection.instance().getSession();
        MCRXMLTableManager manager = MCRXMLTableManager.instance();
        Criteria criteria = session.createCriteria(MCRXMLTABLE.class).add(Restrictions.eq("key.id", mcrId.toString()));
        @SuppressWarnings("unchecked")
        List<MCRXMLTABLE> results = criteria.list();
        for (MCRXMLTABLE xmlentry : results) {
            Date lastModified = xmlentry.getLastModified();
            byte[] xmlByteArray = xmlentry.getXmlByteArray();
            if (super.exists(id)) {
                LOGGER.warn(xmlentry.getId() + " allready exists in IFS2 - skipping.");
                continue;
            }
            LOGGER.info("Migrating " + xmlentry.getId() + " to IFS2.");
            manager.create(mcrId, xmlByteArray, lastModified);
            session.evict(xmlentry);
        }
        if (results.size() == 0) {
            LOGGER.warn("Requested object could not be found in MCRXMLTable: " + mcrId);
        }
    }

}
