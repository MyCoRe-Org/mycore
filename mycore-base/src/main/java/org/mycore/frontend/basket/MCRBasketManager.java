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

import org.jdom.Document;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;

/**
 * Creates and stores Basket objects in the user's current MCRSession.
 * The session may store multiple baskets with different type IDs,
 * for example a basket for documents and another for an other type of entry.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRBasketManager {

    public static MCRBasket getOrCreateBasketInSession(String type) {
        MCRBasket basket = getBasketFromSession(type);
        if (basket == null) {
            basket = new MCRBasket(type);
            setBasketInSession(basket);
        }
        return basket;
    }

    public static MCRBasket getBasketFromSession(String type) {
        String key = getBasketKey(type);
        return (MCRBasket) (MCRSessionMgr.getCurrentSession().get(key));
    }

    public static void setBasketInSession(MCRBasket basket) {
        String key = getBasketKey(basket.getType());
        MCRSessionMgr.getCurrentSession().put(key, basket);
    }

    private static String getBasketKey(String type) {
        return "basket." + type;
    }

    public static MCRBasket loadBasket(String derivateID) throws Exception {
        MCRDirectory dir = (MCRDirectory) (MCRFilesystemNode.getRootNode(derivateID));
        MCRFile file = (MCRFile) (dir.getChild("basket.xml"));
        Document xml = file.getContentAsJDOM();
        return new MCRBasketXMLParser().parseXML(xml);
    }

    public static void updateBasket(MCRBasket basket, String derivateID) throws Exception {
        MCRDirectory dir = (MCRDirectory) (MCRFilesystemNode.getRootNode(derivateID));
        MCRFile file = (MCRFile) (dir.getChild("basket.xml"));
        Document xml = new MCRBasketXMLBuilder(false).buildXML(basket);
        file.setContentFrom(xml);
    }
}
