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
import java.util.HashMap;
import java.util.Map;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.iview2.services.MCRIView2Tools;
import org.mycore.iview2.services.MCRJobState;
import org.mycore.iview2.services.MCRTileJob;
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

    private static SessionFactory sessionFactory = MCRHIBConnection.instance().getSessionFactory();

    private static Logger LOGGER = Logger.getLogger(MCRIView2RemoteFunctions.class);

    /**
     * Asks web service to get next image in the tiling queue.
     * @return Info about next image to be tiled
     * @throws Exception 
     */
    @WebMethod(operationName = "next-tile-job")
    @WebResult(name = "tile-job")
    public MCRIView2RemoteJob getNextTileParameters() throws Exception {
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();
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
            session.close();
        }
    }

    /**
     * Tells web service that images is tiled and available in its directory.
     * @param jobInfo complete info about the finished tiling job
     */
    @WebMethod(operationName = "finish-tile-job")
    public void finishTileJob(@WebParam(name = "job") MCRIView2RemoteJob jobInfo) {
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();
        try {
            MCRTileJob image = null;
            Map<String, String> restrictions = new HashMap<String, String>();
            restrictions.put("derivate", jobInfo.getDerivateID());
            restrictions.put("path", jobInfo.getDerivatePath());
            Criteria jobCriteria = session.createCriteria(MCRTileJob.class).add(Restrictions.allEq(restrictions));
            image = (MCRTileJob) jobCriteria.uniqueResult();
            image.setFinished(new Date());
            image.setStatus(MCRJobState.FINISHED);
            image.setHeight(jobInfo.getHeight());
            image.setWidth(jobInfo.getWidth());
            image.setTiles(jobInfo.getTiles());
            image.setZoomLevel(jobInfo.getZoomLevel());
            session.update(image);
            transaction.commit();
        } catch (HibernateException e) {
            LOGGER.error("Error while getting next tiling job.", e);
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        } finally {
            session.close();
        }
    }

}
