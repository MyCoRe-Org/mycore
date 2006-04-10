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
import org.mycore.datamodel.classifications.MCRCategoryItem;
import org.mycore.datamodel.classifications.MCRClassificationInterface;
import org.mycore.datamodel.classifications.MCRClassificationItem;

/**
 * This class implements the MCRClassificationInterface
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
        MCRCLASS c = new MCRCLASS(classification.getID());

        try {
            session.saveOrUpdate(c);

            for (int i = 0; i < classification.getSize(); i++) {
                MCRCLASSLABEL cl = new MCRCLASSLABEL(classification.getID(), classification.getLang(i), classification.getText(i), classification.getDescription(i));
                session.saveOrUpdate(cl);
            }

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            logger.error(e);
        } finally {
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
        MCRClassificationItem c = new MCRClassificationItem(ID);

        try {
            List l = session.createQuery("FROM MCRCLASS WHERE id = '" + ID + "'").list();

            if (l.size() < 1) {
                session.close();
                logger.error("no classfication with id " + ID);

                return null;
            }

            l = session.createQuery("from MCRCLASSLABEL where ID = '" + ID + "'").list();

            for (int t = 0; t < l.size(); t++) {
                MCRCLASSLABEL cl = (MCRCLASSLABEL) l.get(t);
                c.addData(cl.getLang(), cl.getText(), cl.getMcrdesc());
            }
        } catch (Exception e) {
            logger.error(e);
        } finally {
            session.close();
        }

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
            MCRCATEG c = new MCRCATEG(category.getID(), category.getClassificationID(), category.getParentID(), category.getURL());
            session.saveOrUpdate(c);

            for (int i = 0; i < category.getSize(); i++) {
                MCRCATEGLABEL cl = new MCRCATEGLABEL(category.getID(), category.getClassificationID(), category.getLang(i), category.getText(i), category.getDescription(i));
                session.saveOrUpdate(cl);
            }

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            logger.error(e);
        } finally {
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

            if (l.size() < 1) {
              logger.error("no such category: " + ID + "/" + CLID);

                return null;
            }

            MCRCATEG c = (MCRCATEG) l.get(0);

            ci = new MCRCategoryItem(ID, CLID, c.getPid());
            ci.setURL(c.getUrl());

            List la = session.createQuery("from MCRCATEGLABEL where ID = '" + ID + "' and CLID = '" + CLID + "'").list();

            if (la.size() < 1) {
                logger.error("no such category: " + ID + "/" + CLID);

                return null;
            }

            int t;

            for (t = 0; t < la.size(); t++) {
                MCRCATEGLABEL cl = (MCRCATEGLABEL) la.get(t);
                ci.addData(cl.getLang(), cl.getText(), cl.getMcrdesc());
            }
        } catch (Exception e) {
            logger.error(e);
            throw new MCRException("error while reading category item", e);
        } finally {
            session.close();
        }

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

            if (l.size() < 1) {
                session.close();

                return null;
            }

            MCRCATEG c = (MCRCATEG) l.get(0);
            String ID = c.getId();
            ci = new MCRCategoryItem(ID, CLID, c.getPid());
            ci.setURL(c.getUrl());

            List la = session.createQuery("FROM MCRCATEGLABEL WHERE ID = '" + ID + "' AND CLID = '" + CLID + "'").list();

            if (la.size() < 1) {
                session.close();

                return null;
            }

            for (int t = 0; t < la.size(); t++) {
                MCRCATEGLABEL cl = (MCRCATEGLABEL) la.get(t);
                ci.addData(cl.getLang(), cl.getText(), cl.getMcrdesc());
            }
        } catch (Exception e) {
            logger.error(e);
            throw new MCRException("error while reading categories", e);
        } finally {
            session.close();
        }

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
        ArrayList children = new ArrayList();
        Session session = getSession();

        try {
            List l = session.createQuery("from MCRCATEG where PID = " + ((PID != null) ? ("'" + PID + "'") : "null") + " and CLID = '" + CLID + "'").list();

            for (int t = 0; t < l.size(); t++) {
                MCRCATEG c = (MCRCATEG) l.get(t);
                MCRCategoryItem child = new MCRCategoryItem(c.getId(), c.getClid(), c.getPid());
                child.setURL(c.getUrl());
                children.add(child);
            }

            for (int i = 0; i < children.size(); i++) {
                MCRCategoryItem child = (MCRCategoryItem) (children.get(i));
                List li = session.createQuery("from MCRCATEGLABEL where ID = '" + child.getID() + "' and CLID = '" + CLID + "'").list();

                for (int t = 0; t < li.size(); t++) {
                    MCRCATEGLABEL cl = (MCRCATEGLABEL) li.get(t);
                    child.addData(cl.getLang(), cl.getText(), cl.getMcrdesc());
                }
            }
        } catch (Exception e) {
            logger.error(e);
            throw new MCRException("error while retrieving children of CATEG " + PID, e);
        } finally {
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
    	MCRClassificationItem[] classList =null;
    	
        try {
        	//SELECT count * from MCRCLASS !!! ist redundanzfrei!
            List lcount = session.createQuery("from MCRCLASS").list();
            classList = new MCRClassificationItem[lcount.size()];
            
        	//SELECT id, lang, text, mcrdesc FROM MCRCLASSLABEL M order by id, lang, !! ist mehr als count * from MCRCLASS
            List l = session.createQuery("from MCRCLASSLABEL ORDER BY 1,2").list();
            
            int k = -1;

            for(int i=0; i < classList.length; i++) {
            	MCRCLASSLABEL actual = (MCRCLASSLABEL)l.get(i);
            	if ( k==-1 || !classList[k].getClassificationID().equalsIgnoreCase(actual.getId()) ){
            		k++;
                    logger.debug("next ID of classList[" + Integer.toString(k) + "] = " + actual.getId());
            		classList[k] = new MCRClassificationItem(actual.getId());
               		logger.debug("add first data of classList[" + Integer.toString(k) + "] = " + actual.getId());
               	 	classList[k].addData(actual.getLang(),actual.getText(),actual.getMcrdesc());
            	} else {
            		logger.debug("add more data of classList[" + Integer.toString(k) + "] = " + actual.getId());
               	 	classList[k].addData(actual.getLang(),actual.getText(),actual.getMcrdesc());
            	}
            }
        } catch (Exception e) {
            logger.error(e);
            throw new MCRException("error while retrieving classifications ", e);
        } finally {
            session.close();
        }
        return classList;
    }

}
