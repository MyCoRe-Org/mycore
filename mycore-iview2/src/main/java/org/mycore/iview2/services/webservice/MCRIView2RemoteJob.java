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
package org.mycore.iview2.services.webservice;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "MCRIView2RemoteJob")
@XmlAccessorType(XmlAccessType.FIELD)
public class MCRIView2RemoteJob {
    @XmlAttribute(required = true)
    String derivateID, derivatePath, fileSystemPath;

    @XmlAttribute
    int tiles, width, height, zoomLevel;

    public MCRIView2RemoteJob() {

    }

    public MCRIView2RemoteJob(String derID, String derPath, String imagePath) {
        this.derivateID = derID;
        this.derivatePath = derPath;
        this.fileSystemPath = imagePath;
    }

    public String getDerivateID() {
        return derivateID;
    }

    public String getDerivatePath() {
        return derivatePath;
    }

    public String getFileSystemPath() {
        return fileSystemPath;
    }

    public int getTiles() {
        return tiles;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

}
