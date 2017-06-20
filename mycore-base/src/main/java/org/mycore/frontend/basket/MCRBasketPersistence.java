/*
 * $Revision$ 
 * $Date$
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

package org.mycore.frontend.basket;

import java.io.IOException;
import java.nio.file.Files;

import org.jdom2.Document;
import org.jdom2.output.XMLOutputter;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRPathContent;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * Manages basket objects in the persistent store.
 * A basket can be saved to and loaded from a derivate. The persistent form
 * of a basket is a file "basket.xml" in a derivate.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRBasketPersistence {

    /**
    * Retrieves a basket from an XML file in the given derivate.
    */
    public static MCRBasket retrieveBasket(String derivateID) throws Exception {
        MCRPath file = getBasketFile(derivateID);
        Document xml = new MCRPathContent(file).asXML();
        MCRBasket basket = new MCRBasketXMLParser().parseXML(xml);
        basket.setDerivateID(derivateID);
        return basket;
    }

    /**
     * Returns the MCRFile that stores the persistent data of a basket within the given derivate.
     */
    private static MCRPath getBasketFile(String derivateID) {
        return MCRPath.getPath(derivateID, "/basket.xml");
    }

    /**
     * Updates the basket's data in the persistent store by saving its XML representation
     * to a file in a derivate. The ID of the derivate is given in the basket's properties. 
     */
    public static void updateBasket(MCRBasket basket) throws Exception {
        String derivateID = basket.getDerivateID();
        MCRObjectID derivateOID = MCRObjectID.getInstance(derivateID);
        MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateOID);
        MCRPath file = getBasketFile(derivateID);
        writeBasketToFile(basket, derivate, file);
    }

    /**
     * Creates a new derivate including a file basket.xml which stores the persistent
     * data of the given basket. 
     * 
     * @param basket the basket to store in a new file in a new derivate
     * @param ownerID the ID of the MCRObject owning the new derivate 
     * @throws MCRAccessException see {@link MCRMetadataManager#create(MCRDerivate)}
     * @throws MCRPersistenceException 
     */
    public static void createDerivateWithBasket(MCRBasket basket, MCRObjectID ownerID)
        throws IOException, MCRPersistenceException, MCRAccessException {
        String base = ownerID.getProjectId() + "_derivate";
        MCRObjectID derivateOID = MCRObjectID.getNextFreeId(base);
        String derivateID = derivateOID.toString();

        MCRDerivate derivate = createNewDerivate(ownerID, derivateOID);

        basket.setDerivateID(derivateID);
        writeBasketToFile(basket, derivate, getBasketFile(derivateID));
    }

    /**
     * Creates a new, empty derivate.
     * 
     * @param ownerID the ID of the object owning the new derivate
     * @param derivateOID a free derivate ID to use for the newly created derivate
     * @return the empty derivate that was created.
     * @throws IOException 
     * @throws MCRPersistenceException 
     * @throws MCRAccessException see {@link MCRMetadataManager#create(MCRDerivate)}
     */
    private static MCRDerivate createNewDerivate(MCRObjectID ownerID, MCRObjectID derivateOID)
        throws MCRPersistenceException, IOException, MCRAccessException {
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(derivateOID);
        derivate.setLabel("Saved basket data for " + ownerID.toString());

        String schema = MCRConfiguration.instance().getString("MCR.Metadata.Config.derivate", "datamodel-derivate.xml");
        derivate.setSchema(schema.replaceAll(".xml", ".xsd"));

        MCRMetaIFS ifs = new MCRMetaIFS();
        ifs.setSubTag("internal");
        ifs.setSourcePath(null);
        derivate.getDerivate().setInternals(ifs);

        MCRMetaLinkID linkId = new MCRMetaLinkID();
        linkId.setSubTag("linkmeta");
        linkId.setReference(ownerID, null, null);
        derivate.getDerivate().setLinkMeta(linkId);

        MCRMetadataManager.create(derivate);
        return derivate;
    }

    /**
     * Writes the basket's content to a persistent file in the given derivate.
     * 
     * @param basket the basket to save.
     * @param derivate the derivate holding the file
     * @param basketFile the file holding the basket's data.
     * @throws IOException 
     */
    private static void writeBasketToFile(MCRBasket basket, MCRDerivate derivate, MCRPath basketFile)
        throws IOException {
        Document xml = new MCRBasketXMLBuilder(false).buildXML(basket);
        XMLOutputter xout = new XMLOutputter();
        xout.output(xml, Files.newOutputStream(basketFile));
        MCRMetadataManager.updateMCRDerivateXML(derivate);
    }
}
