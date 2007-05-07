/*
 * $RCSfile$
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.tables.MCRCATEG;
import org.mycore.backend.hibernate.tables.MCRCATEGLABEL;
import org.mycore.backend.hibernate.tables.MCRCLASS;
import org.mycore.backend.hibernate.tables.MCRCLASSLABEL;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUsageException;
import org.mycore.datamodel.classifications.MCRCategoryItem;
import org.mycore.datamodel.classifications.MCRClassificationInterface;
import org.mycore.datamodel.classifications.MCRClassificationItem;
import org.mycore.datamodel.classifications.MCRLabel;
import org.mycore.datamodel.classifications.MCRLink;

/**
 * This class implements the MCRClassificationInterface
 * 
 * @author Heiko Helmbrecht
 * @author Jens Kupferschmidt
 * 
 * @version $Revision$ $Date$
 */
public class MCRHIBClassificationStore implements MCRClassificationInterface {
    // logger
    static Logger logger = Logger.getLogger(MCRHIBClassificationStore.class);

    /**
     * The constructor for the class MCRSQLClassificationStore. It reads the
     * classification configuration and checks the table names.
     */
    public MCRHIBClassificationStore() {
    }

    public final void dropTables() {
        // not supported by hibernate
    }

    public Session getSession() {
        return MCRHIBConnection.instance().getSession();
    }

    /**
     * The method create a new MCRClassificationItem in the datastore.
     * 
     * @param classification
     *            an instance of a MCRClassificationItem
     */
    public final void createClassificationItem(MCRClassificationItem classification) {
        Session session = getSession();
        Transaction tx = session.beginTransaction();

        try {
            MCRCLASS c = new MCRCLASS(classification.getId());
            session.saveOrUpdate(c);
            ArrayList<MCRLabel> label = (ArrayList<MCRLabel>) classification.getLabels();
            for (int i = 0; i < label.size(); i++) {
                MCRCLASSLABEL cl = new MCRCLASSLABEL(classification.getId(), ((MCRLabel) label.get(i)).getLang(), ((MCRLabel) label.get(i)).getText(), ((MCRLabel) label.get(i)).getDescription());
                session.saveOrUpdate(cl);
            }

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            logger.error(e);
        } finally {
            if (session != null)
                session.close();
        }
    }

    private void delete(Session session, String query) {
        List l = session.createQuery(query).list();
        for (int t = 0; t < l.size(); t++) {
            session.delete(l.get(t));
        }
    }

    /**
     * The method remove a MCRClassificationItem from the datastore.
     * 
     * @param ID
     *            the ID of the MCRClassificationItem
     */
    public void deleteClassificationItem(String ID) {
        Session session = getSession();
        Transaction tx = session.beginTransaction();

        try {
            delete(session, "from MCRCLASS where id = '" + ID + "'");
            delete(session, "from MCRCLASSLABEL where ID = '" + ID + "'");
            delete(session, "from MCRCATEG where clid = '" + ID + "'");
            delete(session, "from MCRCATEGLABEL where CLID = '" + ID + "'");

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            logger.error(e);
        } finally {
            if (session != null)
                session.close();
        }
    }

    /**
     * The method return a MCRClassificationItem from the datastore.
     * 
     * @param ID
     *            the ID of the MCRClassificationItem
     */
    public final MCRClassificationItem retrieveClassificationItem(String ID) {
        Session session = getSession();
        MCRClassificationItem c = new MCRClassificationItem();
        c.setId(ID);
        c.setCatgegories(new ArrayList<MCRCategoryItem>());

        try {
            List l = session.createQuery("FROM MCRCLASS WHERE id = '" + ID + "'").list();

            if (l.size() > 0) {
                l = session.createQuery("from MCRCLASSLABEL where ID = '" + ID + "'").list();
                for (int t = 0; t < l.size(); t++) {
                    MCRCLASSLABEL cl = (MCRCLASSLABEL) l.get(t);
                    MCRLabel label = new MCRLabel(cl.getLang(), cl.getText(), cl.getMcrdesc());
                    c.addLabel(label);
                }
            } else
                c = null;

        } catch (Exception e) {
            logger.error(e);
        } finally {
            if (session != null)
                session.close();
        }

        if (null == c)
            throw new MCRUsageException("no classfication with ID '" + ID + "' found");

        return c;
    }

    /**
     * The method returns whether the MCRClassificationItem is in the datastore.
     * 
     * @param ID
     *            the ID of the MCRClassificationItem
     * @return true if the MCRClassificationItem was found, else false
     */
    public final boolean classificationItemExists(String ID) {
        Session session = getSession();
        List l = new LinkedList();

        try {
            l = session.createQuery("from MCRCLASS where ID = '" + ID + "'").list();
        } catch (Exception e) {
            logger.error(e);

            return false; // FIXME: should we throw an exception here?
        } finally {
            if (session != null)
                session.close();
        }

        if (l.size() < 1) {
            return false;
        }

        return true;
    }

    /**
     * The method create a new MCRCategoryItem in the datastore.
     * 
     * @param category
     *            an instance of a MCRCategoryItem
     */
    public final void createCategoryItem(MCRCategoryItem category) {
        Session session = getSession();
        Transaction tx = session.beginTransaction();

        try {
            String url = (category.getLink() != null) ? category.getLink().getHref() : "";
            MCRCATEG c = new MCRCATEG(category.getId(), category.getClassID(), category.getParentID(), url);
            session.saveOrUpdate(c);
            ArrayList<MCRLabel> labellist = (ArrayList<MCRLabel>) category.getLabels();
            for (int i = 0; i < labellist.size(); i++) {
                MCRLabel label = (MCRLabel) labellist.get(i); 
                String text = (label.getText() != null) ? label.getText() : "";
                String desc = (label.getDescription() != null) ? label.getDescription() : "";
                MCRCATEGLABEL cl = new MCRCATEGLABEL(category.getId(), category.getClassID(), label.getLang(), text, desc);
                session.saveOrUpdate(cl);
            }
            tx.commit();
        } catch (Exception e) {
            e.printStackTrace();
            tx.rollback();
            logger.error(e);
        } finally {
            if (session != null)
                session.close();
        }
    }

    /**
     * The method remove a MCRCategoryItem from the datastore.
     * 
     * @param CLID
     *            the ID of the MCRClassificationItem
     * @param ID
     *            the ID of the MCRCategoryItem
     */
    public final void deleteCategoryItem(String CLID, String ID) {
        Session session = getSession();
        Transaction tx = session.beginTransaction();

        try {
            delete(session, "from MCRCATEG where CLID = '" + CLID + "' and ID = '" + ID + "'");
            delete(session, "from MCRCATEGLABEL where CLID = '" + CLID + "' and ID = '" + ID + "'");
            tx.commit();
        } catch (MCRException e) {
            tx.rollback();
            logger.error(e);
        } finally {
            if (session != null)
                session.close();
        }
    }

    /**
     * The method return a MCRCategoryItem from the datastore.
     * 
     * @param CLID
     *            the ID of the MCRClassificationItem
     * @param ID
     *            the ID of the MCRCategoryItem
     */
    public final MCRCategoryItem retrieveCategoryItem(String CLID, String ID) {
        Session session = getSession();
        MCRCategoryItem ci = null;

        try {
            List l = session.createQuery("from MCRCATEG where ID = '" + ID + "' AND CLID = '" + CLID + "'").list();

            if (l.size() > 0) {
                MCRCATEG c = (MCRCATEG) l.get(0);

                ci = new MCRCategoryItem();
                ci.setId(ID);
                ci.setClassID(CLID);
                ci.setParentID(c.getPid());
                ci.setCatgegories(new ArrayList<MCRCategoryItem>());
                MCRLink link = new MCRLink("locator", c.getUrl(), c.getUrl(), "");
                ci.setLink(link);

                List la = session.createQuery("from MCRCATEGLABEL where ID = '" + ID + "' and CLID = '" + CLID + "'").list();

                if (la.size() > 0) {
                    for (int t = 0; t < la.size(); t++) {
                        MCRCATEGLABEL cl = (MCRCATEGLABEL) la.get(t);
                        MCRLabel label = new MCRLabel(cl.getLang(), cl.getText(), cl.getMcrdesc());
                        ci.addLabel(label);
                    }
                } else
                    ci = null;
            }

        } catch (Exception e) {
            logger.error(e);
            throw new MCRException("error while reading category item", e);
        } finally {
            if (session != null)
                session.close();
        }

        if (null == ci)
            throw new MCRUsageException("no such category '" + ID + "' in classification '" + CLID + "' found");

        return ci;
    }

    /**
     * The method return a MCRCategoryItem from the datastore.
     * 
     * @param CLID
     *            the ID of the MCRClassificationItem
     * @param labeltext
     *            the label text of the MCRCategoryItem
     */
    public MCRCategoryItem retrieveCategoryItemForLabelText(String CLID, String labeltext) {
        Session session = getSession();
        MCRCategoryItem ci = null;

        try {
            List l = session.createQuery("FROM MCRCATEG WHERE TEXT = '" + labeltext + "' AND CLID = '" + CLID + "'").list();

            if (l.size() > 0) {
                MCRCATEG c = (MCRCATEG) l.get(0);
                String ID = c.getId();
                ci = new MCRCategoryItem();
                ci.setId(ID);
                ci.setClassID(CLID);
                ci.setParentID(c.getPid());
                ci.setCatgegories(new ArrayList<MCRCategoryItem>());
                MCRLink link = new MCRLink("locator", c.getUrl(), c.getUrl(), "");
                ci.setLink(link);

                List la = session.createQuery("FROM MCRCATEGLABEL WHERE ID = '" + ID + "' AND CLID = '" + CLID + "'").list();

                if (la.size() > 0) {
                    for (int t = 0; t < la.size(); t++) {
                        MCRCATEGLABEL cl = (MCRCATEGLABEL) la.get(t);
                        MCRLabel label = new MCRLabel(cl.getLang(), cl.getText(), cl.getMcrdesc());
                        ci.addLabel(label);
                    }
                } else
                    ci = null;

            }

        } catch (Exception e) {
            logger.error(e);
            throw new MCRException("error while reading categories", e);
        } finally {
            if (session != null)
                session.close();
        }

        if (null == ci)
            throw new MCRUsageException("no such category with label'" + labeltext + "' in classification '" + CLID + "' found");

        return ci;
    }

    /**
     * The method return if the MCRCategoryItem is in the datastore.
     * 
     * @param CLID
     *            the ID of the MCRClassificationItem
     * @param ID
     *            the ID of the MCRCategoryItem
     * @return true if the MCRCategoryItem was found, else false
     */
    public final boolean categoryItemExists(String CLID, String ID) {
        Session session = getSession();
        boolean ret = false;

        try {
            if (session.createQuery("from MCRCATEG where ID = '" + ID + "' AND CLID = '" + CLID + "'").list().size() > 0) {
                ret = true;
            }
        } catch (Exception e) {
            logger.error(e);

            return false;
        } finally {
            if (session != null)
                session.close();
        }

        return ret;
    }

    /**
     * The method return an Vector of MCRCategoryItems from the datastore.
     * 
     * @param CLID
     *            the ID of the MCRClassificationItem
     * @param PID
     *            the parent ID of the MCRCategoryItem
     * @return a list of MCRCategoryItem children
     */
    public final ArrayList retrieveChildren(String CLID, String PID) {
        ArrayList<MCRCategoryItem> children = new ArrayList<MCRCategoryItem>();
        Session session = getSession();

        try {
            List l = session.createQuery("from MCRCATEG where PID = " + ((PID != null) ? ("'" + PID + "'") : "null") + " and CLID = '" + CLID + "'").list();

            for (int t = 0; t < l.size(); t++) {
                MCRCATEG c = (MCRCATEG) l.get(t);
                MCRCategoryItem child = new MCRCategoryItem();
                child.setId(c.getId());
                child.setClassID(c.getClid());
                child.setParentID(c.getPid());
                MCRLink link = new MCRLink("locator", c.getUrl(), c.getUrl(), "");
                child.setLink(link);
                children.add(child);
            }

            for (int i = 0; i < children.size(); i++) {
                MCRCategoryItem child = (MCRCategoryItem) (children.get(i));
                List li = session.createQuery("from MCRCATEGLABEL where ID = '" + child.getId() + "' and CLID = '" + CLID + "'").list();

                for (int t = 0; t < li.size(); t++) {
                    MCRCATEGLABEL cl = (MCRCATEGLABEL) li.get(t);
                    MCRLabel label = new MCRLabel(cl.getLang(), cl.getText(), cl.getMcrdesc());
                    child.addLabel(label);
                }
            }
        } catch (Exception e) {
            logger.error(e);
            throw new MCRException("error while retrieving children of CATEG " + PID, e);
        } finally {
            if (session != null)
                session.close();
        }

        return children;
    }

    /**
     * The method return the number of MCRCategoryItems from the datastore.
     * 
     * @param CLID
     *            the ID of the MCRClassificationItem
     * @param PID
     *            the parent ID of the MCRCategoryItem
     * @return the number of MCRCategoryItem children
     */
    public final int retrieveNumberOfChildren(String CLID, String PID) {
        return retrieveChildren(CLID, PID).size();
    }

    /**
     * The method returns all availiable classification ID's they are loaded.
     * 
     * @return a list of classification ID's as String array
     */
    public final String[] getAllClassificationID() {
        Session session = getSession();
        String[] ID = null;

        try {
            List l = session.createQuery("from MCRCLASS").list();
            ID = new String[l.size()];

            for (int i = 0; i < ID.length; i++) {
                ID[i] = ((MCRCLASS) l.get(i)).getId();
                logger.debug("ID of classifications[" + Integer.toString(i) + "] = " + ID[i]);
            }
        } catch (Exception e) {
            logger.error(e);
            throw new MCRException("error while retrieving classification IDs", e);
        } finally {
            if (session != null)
                session.close();
        }

        return ID;
    }

    /**
     * The method returns all availiable classification's they are loaded.
     * 
     * @return a list of classification ID's as String array
     */
    public final MCRClassificationItem[] getAllClassification() {
        Session session = getSession();
        logger.debug("List of classifications");
        MCRClassificationItem[] classList = null;

        try {
            // SELECT count * from MCRCLASS !!! ist redundanzfrei!
            List lcount = session.createQuery("from MCRCLASS").list();
            classList = new MCRClassificationItem[lcount.size()];

            // SELECT id, lang, text, mcrdesc FROM MCRCLASSLABEL M order by id,
            // lang, !! ist mehr als count * from MCRCLASS
            List l = session.createQuery("from MCRCLASSLABEL ORDER BY 1,2").list();

            int k = -1;

            for (int i = 0; i < l.size(); i++) {
                MCRCLASSLABEL actual = (MCRCLASSLABEL) l.get(i);
                if (k == -1 || !classList[k].getId().equalsIgnoreCase(actual.getId())) {
                    k++;
                    logger.debug("next ID of classList[" + Integer.toString(k) + "] = " + actual.getId());
                    classList[k] = new MCRClassificationItem();
                    classList[k].setId(actual.getId());
                    logger.debug("add first data of classList[" + Integer.toString(k) + "] = " + actual.getId());
                    MCRLabel label = new MCRLabel(actual.getLang(), actual.getText(), actual.getMcrdesc());
                    classList[k].addLabel(label);
                } else {
                    logger.debug("add more data of classList[" + Integer.toString(k) + "] = " + actual.getId());
                    MCRLabel label = new MCRLabel(actual.getLang(), actual.getText(), actual.getMcrdesc());
                    classList[k].addLabel(label);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new MCRException("error while retrieving classifications ", e);
        } finally {
            if (session != null)
                session.close();
        }
        return classList;
    }

}
