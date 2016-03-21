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
package org.mycore.services.queuedjob;

import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;

/**
 * A slave thread of {@link MCRJobMaster}.
 * 
 * This class execute the specified action for {@link MCRJob} and performs {@link MCRJobAction#rollback()} 
 * if an error occurs. 
 *
 * @author Ren\u00E9 Adler
 *
 */
public class MCRJobThread implements Runnable {
    protected MCRJob job = null;

    private static Logger LOGGER = Logger.getLogger(MCRJobThread.class);

    public MCRJobThread(MCRJob job) {
        this.job = job;
    }

    public void run() {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        mcrSession.setUserInformation(MCRSystemUserInformation.getSystemUserInstance());
        Session session = MCRHIBConnection.instance().getSession();
        Transaction transaction = session.beginTransaction();
        try {
            Class<? extends MCRJobAction> actionClass = job.getAction();
            Constructor<? extends MCRJobAction> actionConstructor = actionClass.getConstructor(MCRJob.class);
            MCRJobAction action = actionConstructor.newInstance(job);

            try {
                job.setStart(new Date());

                action.execute();

                job.setFinished(new Date());
                job.setStatus(MCRJobStatus.FINISHED);
            } catch (ExecutionException ex) {
                LOGGER.error("Exception occured while try to start job. Perform rollback.", ex);
                action.rollback();
            } catch (Exception e) {
                LOGGER.error("Exception occured while try to start job.", e);
            }
            session.update(job);
            transaction.commit();
        } catch (Exception e) {
            LOGGER.error("Error while getting next job.", e);
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            session.close();
            MCRSessionMgr.releaseCurrentSession();
            mcrSession.close();
        }
    }
}
