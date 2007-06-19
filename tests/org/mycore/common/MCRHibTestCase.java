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
package org.mycore.common;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import org.mycore.backend.hibernate.MCRHIBConnection;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * Need to insert some things here
 * 
 */
public class MCRHibTestCase extends MCRTestCase {

    protected SessionFactory sessionFactory;

    protected Transaction tx;

    @Override
    protected void setUp() throws Exception {
        // Configure logging etc.
        super.setUp();
        boolean setPropertie=false;
        setPropertie=setProperty("log4j.logger.org.hibernate","WARN",false)?true:setPropertie;
        setPropertie=setProperty("log4j.logger.org.hsqldb","WARN",false)?true:setPropertie;
        if (setPropertie){
            CONFIG.configureLogging();
        }
        System.setProperty("MCR.Hibernate.Configuration", "org/mycore/hibernate.cfg.xml");
        sessionFactory = MCRHIBConnection.instance().getSessionFactory();
        tx = sessionFactory.getCurrentSession().beginTransaction();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (tx != null && tx.isActive()) {
            try {
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
            }
        }
    }

}
