/*
 * $Id$
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

package org.mycore.iview2.services.webservice;

import java.io.IOException;
import java.util.Date;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.iview2.services.MCRIView2Tools;
import org.mycore.iview2.services.MCRJobState;
import org.mycore.iview2.services.MCRTileJob;
import org.mycore.iview2.services.MCRTileJob_;
import org.mycore.iview2.services.MCRTilingQueue;

/**
 * WebServices from IView2.
 * @author Thomas Scheffler (yagee)
 *
 */
@WebService(targetNamespace = "http://mycore.org/iview2/services/remoteClient")
@SOAPBinding(style=Style.RPC)
public class MCRIView2RemoteFunctions {
    private static MCRTilingQueue TILE_QUEUE = MCRTilingQueue.getInstance();

    private static EntityManagerFactory emFactory = MCREntityManagerProvider.getEntityManagerFactory();

    private static Logger LOGGER = Logger.getLogger(MCRIView2RemoteFunctions.class);

    /**
     * Asks web service to get next image in the tiling queue.
     * @return Info about next image to be tiled
     */
    @WebMethod(operationName = "next-tile-job")
    @WebResult(name = "tile-job")
    public MCRIView2RemoteJob getNextTileParameters() throws Exception {
        EntityManager em = emFactory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            MCRTileJob mcrTileJob = TILE_QUEUE.poll();
            if (mcrTileJob == null){
                transaction.commit();
                return new MCRIView2RemoteJob();
            }
            String derID = mcrTileJob.getDerivate();
            String derPath = mcrTileJob.getPath();
            String imagePath = MCRIView2Tools.getFilePath(derID, derPath);
            transaction.commit();
            return new MCRIView2RemoteJob(derID, derPath, imagePath);
        } catch (HibernateException | IOException e) {
            LOGGER.error("Error while getting next tiling job.", e);
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Tells web service that images is tiled and available in its directory.
     * @param jobInfo complete info about the finished tiling job
     */
    @WebMethod(operationName = "finish-tile-job")
    public void finishTileJob(@WebParam(name = "job") MCRIView2RemoteJob jobInfo) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRTileJob> jobQuery = cb.createQuery(MCRTileJob.class);
        Root<MCRTileJob> jobs = jobQuery.from(MCRTileJob.class);
        jobQuery.where(cb.equal(jobs.get(MCRTileJob_.derivate), jobInfo.getDerivateID()), cb.equal(jobs.get(MCRTileJob_.path), jobInfo.getDerivatePath()));
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        try {
            MCRTileJob image = em.createQuery(jobQuery).getSingleResult();
            image.setFinished(new Date());
            image.setStatus(MCRJobState.FINISHED);
            image.setHeight(jobInfo.getHeight());
            image.setWidth(jobInfo.getWidth());
            image.setTiles(jobInfo.getTiles());
            image.setZoomLevel(jobInfo.getZoomLevel());
            transaction.commit();
        } catch (PersistenceException e) {
            LOGGER.error("Error while getting next tiling job.", e);
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

}
