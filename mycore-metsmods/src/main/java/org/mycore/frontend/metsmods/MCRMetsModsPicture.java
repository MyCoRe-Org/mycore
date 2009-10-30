/*
 * 
 * $Revision: 1.1 $ $Date: 2009/01/13 08:41:14 $
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

package org.mycore.frontend.metsmods;

/**
 * @author Stefan Freitag
 * @version $Revision: 1.1 $ $Date: 2009/01/13 08:41:14 $
 */
public class MCRMetsModsPicture {

    String picture;
    int order;
    String orderlabel;

    public MCRMetsModsPicture(String picture, int order, String orderlabel) {
        this.picture = picture;
        this.order = order;
        this.orderlabel = orderlabel;
    }

    public int getOrder() {
        return this.order;
    }

    public String getOrderlabel() {
        return this.orderlabel;
    }

    public String getPicture() {
        return this.picture;
    }

    public void setOrder(int i) {
        this.order = i;
        if ((orderlabel == null) || (orderlabel.length() == 0)) {
            this.orderlabel = String.valueOf(i);
        }
    }

    public void setOrderlabel(String i) {
        this.orderlabel = i;
    }

    public void show() {
        System.out.println("Picture: " + picture + " Order: " + order + " Orderlabel: " + orderlabel);
    }
}
