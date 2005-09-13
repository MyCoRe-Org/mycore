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

package org.mycore.common.events;

import org.apache.log4j.Logger;

import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * Abstract helper class that can be subclassed to implement event handlers more
 * easily.
 * 
 * @author Frank Lützenkirchen
 */
public abstract class MCREventHandlerBase implements MCREventHandler {
	private static Logger logger = Logger.getLogger(MCREventHandlerBase.class);

	public void doHandleEvent(MCREvent evt) {
		if (evt.getObjectType().equals(MCREvent.OBJECT_TYPE)) 
		{
		  MCRObject obj = (MCRObject) (evt.get("object"));  
		  logger.debug(getClass().getName() + " handling "
					+ obj.getId().getId() + evt.getEventType() );
		  
		  if( evt.getEventType().equals(MCREvent.CREATE_EVENT))
		    handleObjectCreated(evt, obj);
		  else if( evt.getEventType().equals(MCREvent.UPDATE_EVENT))
		    handleObjectUpdated(evt, obj);
		  else if( evt.getEventType().equals(MCREvent.DELETE_EVENT))
		    handleObjectDeleted(evt, obj);
		}
		else if (evt.getObjectType().equals(MCREvent.FILE_TYPE)) 
		{
		    MCRFile file = (MCRFile) (evt.get("file"));
			logger.debug(getClass().getName() + " handling "
					+ file.getOwnerID() + "/" + file.getAbsolutePath() + " "
					+ evt.getEventType());

		  if( evt.getEventType().equals(MCREvent.CREATE_EVENT))
		    handleFileCreated(evt, file);
		  else if( evt.getEventType().equals(MCREvent.UPDATE_EVENT))
		    handleFileUpdated(evt, file);
		  else if( evt.getEventType().equals(MCREvent.DELETE_EVENT))
		    handleFileDeleted(evt, file);
		}
	}

	public void undoHandleEvent(MCREvent evt) {
		if (evt.getObjectType().equals(MCREvent.OBJECT_TYPE)) 
		{
		  MCRObject obj = (MCRObject) (evt.get("object"));  
		  logger.debug(getClass().getName() + " handling "
					+ obj.getId().getId() + evt.getEventType() );
		  
		  if( evt.getEventType().equals(MCREvent.CREATE_EVENT))
		    undoObjectCreated(evt, obj);
		  else if( evt.getEventType().equals(MCREvent.UPDATE_EVENT))
		    undoObjectUpdated(evt, obj);
		  else if( evt.getEventType().equals(MCREvent.DELETE_EVENT))
		    undoObjectDeleted(evt, obj);
		}
		else if (evt.getObjectType().equals(MCREvent.FILE_TYPE)) 
		{
		    MCRFile file = (MCRFile) (evt.get("file"));
			logger.debug(getClass().getName() + " handling "
					+ file.getOwnerID() + "/" + file.getAbsolutePath() + " "
					+ evt.getEventType());

		  if( evt.getEventType().equals(MCREvent.CREATE_EVENT))
		    undoFileCreated(evt, file);
		  else if( evt.getEventType().equals(MCREvent.UPDATE_EVENT))
		    undoFileUpdated(evt, file);
		  else if( evt.getEventType().equals(MCREvent.DELETE_EVENT))
		    undoFileDeleted(evt, file);
		}
	}

	/**
	 * Handles object created events. This implementation does nothing and
	 * should be overwritted by subclasses.
	 * 
	 * @param evt
	 *            the event that occured
	 * @param obj
	 *            the MCRObject that caused the event
	 */
	protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
		logger.debug("This default handler implementation does nothing");
	}

	/**
	 * Handles object updated events. This implementation does nothing and
	 * should be overwritted by subclasses.
	 * 
	 * @param evt
	 *            the event that occured
	 * @param obj
	 *            the MCRObject that caused the event
	 */
	protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
		logger.debug("This default handler implementation does nothing");
	}

	/**
	 * Handles object deleted events. This implementation does nothing and
	 * should be overwritted by subclasses.
	 * 
	 * @param evt
	 *            the event that occured
	 * @param obj
	 *            the MCRObject that caused the event
	 */
	protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
		logger.debug("This default handler implementation does nothing");
	}

	/**
	 * Handles file created events. This implementation does nothing and should
	 * be overwritted by subclasses.
	 * 
	 * @param evt
	 *            the event that occured
	 * @param file
	 *            the MCRFile that caused the event
	 */
	protected void handleFileCreated(MCREvent evt, MCRFile file) {
		logger.debug("This default handler implementation does nothing");
	}

	/**
	 * Handles file updated events. This implementation does nothing and should
	 * be overwritted by subclasses.
	 * 
	 * @param evt
	 *            the event that occured
	 * @param file
	 *            the MCRFile that caused the event
	 */
	protected void handleFileUpdated(MCREvent evt, MCRFile file) {
		logger.debug("This default handler implementation does nothing");
	}

	/**
	 * Handles file deleted events. This implementation does nothing and should
	 * be overwritted by subclasses.
	 * 
	 * @param evt
	 *            the event that occured
	 * @param file
	 *            the MCRFile that caused the event
	 */
	protected void handleFileDeleted(MCREvent evt, MCRFile file) {
		logger.debug("This default handler implementation does nothing");
	}
	
	/**
	 * Handles undo of object created events. This implementation does nothing and
	 * should be overwritted by subclasses.
	 * 
	 * @param evt
	 *            the event that occured
	 * @param obj
	 *            the MCRObject that caused the event
	 */
	protected void undoObjectCreated(MCREvent evt, MCRObject obj) {
		logger.debug("This default handler implementation does nothing");
	}

	/**
	 * Handles undo of object updated events. This implementation does nothing and
	 * should be overwritted by subclasses.
	 * 
	 * @param evt
	 *            the event that occured
	 * @param obj
	 *            the MCRObject that caused the event
	 */
	protected void undoObjectUpdated(MCREvent evt, MCRObject obj) {
		logger.debug("This default handler implementation does nothing");
	}

	/**
	 * Handles undo of object deleted events. This implementation does nothing and
	 * should be overwritted by subclasses.
	 * 
	 * @param evt
	 *            the event that occured
	 * @param obj
	 *            the MCRObject that caused the event
	 */
	protected void undoObjectDeleted(MCREvent evt, MCRObject obj) {
		logger.debug("This default handler implementation does nothing");
	}

	/**
	 * Handles undo of file created events. This implementation does nothing and should
	 * be overwritted by subclasses.
	 * 
	 * @param evt
	 *            the event that occured
	 * @param file
	 *            the MCRFile that caused the event
	 */
	protected void undoFileCreated(MCREvent evt, MCRFile file) {
		logger.debug("This default handler implementation does nothing");
	}

	/**
	 * Handles undo of file updated events. This implementation does nothing and should
	 * be overwritted by subclasses.
	 * 
	 * @param evt
	 *            the event that occured
	 * @param file
	 *            the MCRFile that caused the event
	 */
	protected void undoFileUpdated(MCREvent evt, MCRFile file) {
		logger.debug("This default handler implementation does nothing");
	}

	/**
	 * Handles undo of file deleted events. This implementation does nothing and should
	 * be overwritted by subclasses.
	 * 
	 * @param evt
	 *            the event that occured
	 * @param file
	 *            the MCRFile that caused the event
	 */
	protected void undoFileDeleted(MCREvent evt, MCRFile file) {
		logger.debug("This default handler implementation does nothing");
	}
}