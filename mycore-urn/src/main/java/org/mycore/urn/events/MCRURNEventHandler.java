/*
 * 
 * $Revision: 25642 $ $Date: 2012-12-21 11:37:10 +0100 (Fr, 21 Dez 2012) $
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
package org.mycore.urn.events;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.access.MCRAccessException;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRFileMetadata;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.urn.hibernate.MCRURN;
import org.mycore.urn.services.MCRURNManager;

/**
 * This class is responsible for the urn after an object has been deleted in the
 * database
 * 
 * @author shermann
 * @author Robert Stephan
 */
public class MCRURNEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = Logger.getLogger(MCRURNEventHandler.class);

    /**
     * Handles object created events. This method updates the urn store.
     * The urn is retrieved from object metadata with an XPath expression which can be configured by properties
     * MCR.Persistence.URN.XPath.{type} or as a default MCR.Persistence.URN.XPath 
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    @Override
    protected final void handleObjectCreated(MCREvent evt, MCRObject obj) {
        try {
            MCRBaseContent content = new MCRBaseContent(obj);
            Document doc = content.asXML();
            String type = obj.getId().getTypeId();

            MCRConfiguration conf = MCRConfiguration.instance();
            String xPathString = conf.getString("MCR.Persistence.URN.XPath." + type,
                conf.getString("MCR.Persistence.URN.XPath", ""));

            if (!xPathString.isEmpty()) {
                String urn = null;
                XPathExpression<Object> xpath = XPathFactory.instance().compile(xPathString, Filters.fpassthrough(),
                    null, MCRConstants.getStandardNamespaces());
                Object o = xpath.evaluateFirst(doc);
                if (o instanceof Attribute) {
                    urn = ((Attribute) o).getValue();
                }
                //element or text node
                else if (o instanceof Content) {
                    urn = ((Content) o).getValue();
                }

                if (urn != null) {
                    MCRURNManager.assignURN(urn, obj.getId().toString());
                } else {
                    if (MCRURNManager.hasURNAssigned(obj.getId().toString())) {
                        MCRURNManager.removeURNByObjectID(obj.getId().toString());
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Could not store / update the urn for object with id " + obj.getId().toString()
                + " into the database", ex);
        }
    }

    /**
     * Handles object updated events
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    @Override
    protected final void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        handleObjectCreated(evt, obj);
    }

    /**
     * Handles object repaired events
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    @Override
    protected final void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        handleObjectCreated(evt, obj);
    }

    /**
     * Handles object deleted events. This implementation deletes the urn
     * records in the MCRURN table
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        try {
            if (MCRURNManager.hasURNAssigned(obj.getId().toString())) {
                LOGGER.info("Deleting urn from database for object belonging to " + obj.getId().toString());
                MCRURNManager.removeURNByObjectID(obj.getId().toString());
            }

        } catch (Exception ex) {
            LOGGER.error("Could not delete the urn from the database for object with id " + obj.getId().toString(), ex);
        }
    }

    /**
     * Handles derivate deleted events. This implementation deletes the urn
     * records in the MCRURN table
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        try {
            if (MCRURNManager.hasURNAssigned(der.getId().toString())) {
                MCRURNManager.removeURNByObjectID(der.getId().toString());
                LOGGER.info("Deleting urn from database for derivates belonging to " + der.getId().toString());
            }
        } catch (Exception ex) {
            LOGGER.error("Could not delete the urn from the database for object with id " + der.getId().toString(), ex);
        }
    }

    /**
     * Handles derivate created events. This implementation adds the urn records in the MCRURN table
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        MCRObjectID derivateID = der.getId();
        MCRObjectDerivate objectDerivate = der.getDerivate();
        String urn = objectDerivate.getURN();
        if (urn == null || !MCRURNManager.isValid(urn)) {
            return;
        }
        MCRURNManager.assignURN(urn, derivateID.toString());
        List<MCRFileMetadata> fileMetadata = objectDerivate.getFileMetadata();
        for (MCRFileMetadata metadata : fileMetadata) {
            String fileURN = metadata.getUrn();
            if (urn != null) {
                LOGGER.info(MessageFormat.format("load file urn : %s, %s, %s", fileURN, derivateID, metadata.getName())
                    .toString());
                MCRURNManager.assignURN(fileURN, derivateID.toString(), metadata.getName());
            }
        }
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        Criteria q = getSession().createCriteria(MCRURN.class);
        q.add(Restrictions.isNull("path"));
        q.add(Restrictions.isNull("filename"));
        q.add(Restrictions.eq("key.mcrid", der.getId().toString()));

        MCRURN entry = (MCRURN) q.uniqueResult();
        if (entry != null) {
            entry.setDfg(false);
            getSession().update(entry);
        }
        MCREvent indexEvent = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.INDEX_EVENT);
        indexEvent.put("derivate", der);
        MCREventManager.instance().handleEvent(indexEvent);
    }

    private Session getSession() {
        return MCRHIBConnection.instance().getSession();
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleFileDeleted(org.mycore.common.events.MCREvent, org.mycore.datamodel.ifs.MCRFile)
     */
    @Override
    protected void handlePathDeleted(MCREvent evt, Path path, BasicFileAttributes attrs) {
        if (path instanceof MCRPath) {
            String urn = MCRURNManager.getURNForPath(MCRPath.toMCRPath(path));
            if (urn != null) {
                LOGGER.info(MessageFormat.format("Removing urn {0} for file {1} from database", urn, path));
                MCRURNManager.removeURN(urn);
            }
        }
    }

    /**
     * When overriding an existing file with urn, this method ensures the urn remaining in the derivate xml.
     * */
    @Override
    protected void handlePathCreated(MCREvent evt, Path path, BasicFileAttributes attrs) {
        //TODO handle directory structures
        if (!(path instanceof MCRPath)) {
            return;
        }
        if (path.getParent().getParent() != null) {
            LOGGER.warn("Sorry, directories are currently not supported.");
            return;
        }

        MCRPath mcrPath = MCRPath.toMCRPath(path);
        String urn = MCRURNManager.getURNForPath(mcrPath);

        if (urn == null) {
            return;
        }

        MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(mcrPath.getOwner()));
        derivate.getDerivate().getOrCreateFileMetadata(mcrPath, urn);
        try {
            MCRMetadataManager.update(derivate);
        } catch (MCRPersistenceException | MCRAccessException | IOException e) {
            e.printStackTrace();
        }
    }
}
