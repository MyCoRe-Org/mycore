/*
 * $Id$
 * $Revision: 5697 $ $Date: 02.04.2012 $
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

package org.mycore.wfc.mail;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRMailer;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.MCRPathXML;

/**
 * Uses "e-mail-events.xsl" to transform derivate, object and files to emails.
 * 
 * See {@link MCRMailer} for email xml format.
 * 
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRMailEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger(MCRMailEventHandler.class);

    private void sendNotificationMail(MCREvent evt, MCRContent doc, String description) throws Exception {
        LOGGER.info("Preparing mail for: " + description);
        HashMap<String, String> parameters = new HashMap<String, String>();
        for (Map.Entry<String, Object> entry : evt.entrySet()) {
            parameters.put(entry.getKey(), entry.getValue().toString());
        }
        parameters.put("action", evt.getEventType());
        parameters.put("type", evt.getObjectType());

        MCRMailer.sendMail(doc.asXML(), "e-mail-events", parameters);
    }

    private void handleCategoryEvent(MCREvent evt, MCRCategory obj) {
        MCRContent xml = new MCRJDOMContent(MCRCategoryTransformer.getMetaDataDocument(obj, false));
        handleEvent(evt, xml, obj.toString());
    }

    private void handleObjectEvent(MCREvent evt, MCRObject obj) {
        MCRContent xml = new MCRJDOMContent(obj.createXML());
        handleEvent(evt, xml, obj.getId().toString());
    }

    private void handleDerivateEvent(MCREvent evt, MCRDerivate der) {
        MCRContent xml = new MCRJDOMContent(der.createXML());
        handleEvent(evt, xml, der.getId().toString());
    }

    private void handlePathEvent(MCREvent evt, Path file, BasicFileAttributes attrs) {
        if (!(file instanceof MCRPath)) {
            return;
        }
        MCRPath path = MCRPath.toMCRPath(file);
        MCRContent xml;
        try {
            xml = new MCRJDOMContent(MCRPathXML.getFileXML(path, attrs));
            handleEvent(evt, xml, path.toString());
        } catch (IOException e) {
            LOGGER.error("Error while generating mail for " + file, e);
        }
    }

    private void handleEvent(MCREvent evt, MCRContent xml, String description) {
        try {
            sendNotificationMail(evt, xml, description);
        } catch (Exception e) {
            LOGGER.error("Error while handling event: " + evt, e);
        }
    }

    @Override
    protected void handleClassificationCreated(MCREvent evt, MCRCategory obj) {
        handleCategoryEvent(evt, obj);
    }

    @Override
    protected void handleClassificationUpdated(MCREvent evt, MCRCategory obj) {
        handleCategoryEvent(evt, obj);
    }

    @Override
    protected void handleClassificationDeleted(MCREvent evt, MCRCategory obj) {
        handleCategoryEvent(evt, obj);
    }

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        handleObjectEvent(evt, obj);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        handleObjectEvent(evt, obj);
    }

    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        handleObjectEvent(evt, obj);
    }

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        handleDerivateEvent(evt, der);
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        handleDerivateEvent(evt, der);
    }

    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        handleDerivateEvent(evt, der);
    }

    @Override
    protected void handlePathCreated(MCREvent evt, Path file, BasicFileAttributes attrs) {
        handlePathEvent(evt, file, attrs);
    }

    @Override
    protected void handlePathUpdated(MCREvent evt, Path file, BasicFileAttributes attrs) {
        handlePathEvent(evt, file, attrs);
    }

    @Override
    protected void handlePathDeleted(MCREvent evt, Path file, BasicFileAttributes attrs) {
        handlePathEvent(evt, file, attrs);
    }

}
