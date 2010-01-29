/*
 * $Id$
 * $Revision: 5697 $ $Date: 25.01.2010 $
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

package org.mycore.frontend.iview2;

import org.mycore.common.MCRConfiguration;
import org.mycore.services.iview2.MCRIView2Tools;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRIView2XSLFunctions {

    public static boolean hasMETSFile(String derivateID) {
        return (MCRIView2Tools.getMCRFile(derivateID, "/mets.xml") != null);
    }

    public static String getSupportedMainFile(String derivateID) {
        return MCRIView2Tools.getSupportedMainFile(derivateID);
    }

    public static String getThumbnailURL(String derivate, String imagePath) {
        String[] baseURLs = MCRConfiguration.instance().getString("MCR.Module-iview2.BaseURL").split(",");
        int index = imagePath.hashCode() % baseURLs.length;
        StringBuilder baseURL = new StringBuilder(baseURLs[index]);
        baseURL.append('/').append(derivate);
        if (imagePath.charAt(0) != '/')
            baseURL.append('/');
        int dotPos = imagePath.lastIndexOf('.');
        if (dotPos > 0)
            imagePath = imagePath.substring(0, dotPos);
        baseURL.append(imagePath).append(".iview2/0/0/0.jpg");
        return baseURL.toString();
    }
}
