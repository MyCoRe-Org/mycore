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

import org.mycore.common.MCRSessionMgr;

/**
 * Manages basket objects in the user's current MCRSession.
 * A session may store multiple baskets with different type IDs,
 * for example a basket for documents and another for an other type of entry.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRBasketManager {

    /**
     * Convenience method to get a basket of the given type. 
     * When there already is a basket in the session, that basket is returned.
     * Otherwise a new basket is created and saved in the session.
     */
    public static MCRBasket getOrCreateBasketInSession(String type) {
        MCRBasket basket = getBasketFromSession(type);
        if (basket == null) {
            basket = new MCRBasket(type);
            setBasketInSession(basket);
        }
        return basket;
    }

    /**
     * Returns the basket of the given type from the current session, if there is any.
     */
    public static MCRBasket getBasketFromSession(String type) {
        String key = getBasketKey(type);
        return (MCRBasket) (MCRSessionMgr.getCurrentSession().get(key));
    }

    /**
     * Stores the given basket in the current user's session
     */
    public static void setBasketInSession(MCRBasket basket) {
        String key = getBasketKey(basket.getType());
        MCRSessionMgr.getCurrentSession().put(key, basket);
    }

    /**
     * Returns the key to be used to store a basket in the current user's MCRSession
     */
    private static String getBasketKey(String type) {
        return "basket." + type;
    }

    /**
     * Checks if a basket entry is present in the current basket
     * @param type basket type
     * @param id basket entry id
     * @return true if a basket of this type exist and contains basket entry with the given id 
     */
    public static boolean contains(String type, String id) {
        MCRBasket basket = getBasketFromSession(type);
        if (basket == null) {
            return false;
        }
        return (basket.get(id) != null);
    }
}
