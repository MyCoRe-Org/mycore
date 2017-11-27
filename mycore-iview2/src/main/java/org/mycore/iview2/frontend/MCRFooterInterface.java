/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.iview2.frontend;

import java.awt.image.BufferedImage;

/**
 * The MCRFooterInterface adds the possibility to write a different implementations and combine them for example with the {@link MCRTileCombineServlet}.  
 * @author Thomas Scheffler (yagee)
 *
 */
public interface MCRFooterInterface {
    /**
     * generates a footer image that is pinned below the master image
     * 
     * Implementors can use <code>derivateID</code> and <code>imagePath</code> information
     * to add some informations to the image footer, e.g. URNs.
     * @param imageWidth image width of the master image
     * @param derivateID derivate ID
     * @param imagePath path to image relative to derivate root
     * @return an image of any height but with width of <code>imageWidth</code> px.
     */
    BufferedImage getFooter(int imageWidth, String derivateID, String imagePath);
}
