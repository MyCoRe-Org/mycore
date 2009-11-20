/*
 * $Id$
 * $Revision: 5697 $ $Date: 20.11.2009 $
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

package org.mycore.services.iview2.webservice;

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
import org.mycore.services.iview2.MCRIView2Tools;
import org.mycore.services.iview2.MCRJobState;
import org.mycore.services.iview2.MCRTileJob;
import org.mycore.services.iview2.MCRTilingQueue;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@WebService(targetNamespace = "http://mycore.org/services/iview/remoteClient")
@SOAPBinding(style=Style.RPC)
public class MCRIView2RemoteFunctions {
    private static MCRTilingQueue TILE_QUEUE = MCRTilingQueue.getInstance();

    private static SessionFactory sessionFactory = MCRHIBConnection.instance().getSessionFactory();

    private static Logger LOGGER = Logger.getLogger(MCRIView2RemoteFunctions.class);

    @WebMethod(operationName = "next-tile-job")
    @WebResult(name = "tile-job")
    public MCRIView2RemoteJob getNextTileParameters() {
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();
        try {
            MCRTileJob mcrTileJob = TILE_QUEUE.poll();
            transaction.commit();
            if (mcrTileJob == null)
                return null;
            String derID = mcrTileJob.getDerivate();
            String derPath = mcrTileJob.getPath();
            String imagePath = MCRIView2Tools.getFilePath(derID, derPath);
            return new MCRIView2RemoteJob(derID, derPath, imagePath);
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

    @WebMethod(operationName = "finish-tile-job")
    public void finishTileJob(@WebParam(name = "job") MCRIView2RemoteJob pojo) {
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();
        try {
            MCRTileJob image = null;
            Map<String, String> restrictions = new HashMap<String, String>();
            restrictions.put("derivate", pojo.getDerivateID());
            restrictions.put("path", pojo.getDerivatePath());
            Criteria jobCriteria = session.createCriteria(MCRTileJob.class).add(Restrictions.allEq(restrictions));
            image = (MCRTileJob) jobCriteria.uniqueResult();
            image.setFinished(new Date());
            image.setStatus(MCRJobState.FIN);
            image.setHeight(pojo.getHeight());
            image.setWidth(pojo.getWidth());
            image.setTiles(pojo.getTiles());
            image.setZoomLevel(pojo.getZoomLevel());
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
