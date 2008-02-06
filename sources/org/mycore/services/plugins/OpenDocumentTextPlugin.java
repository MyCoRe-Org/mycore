/*
 * 
 * $Revision$ $Date$
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

package org.mycore.services.plugins;


/**
 * @author Thomas Scheffler (yagee)
 * 
 * Need to insert some things here
 * 
 */
public class OpenDocumentTextPlugin extends OpenOfficeBasePlugin {
    private static final int MAJOR = 0;

    private static final int MINOR = 1;

    public OpenDocumentTextPlugin() {
        this("odt");
    }

    private OpenDocumentTextPlugin(String contentType) {
        super(contentType);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.plugins.TextFilterPlugin#getName()
     */
    public String getName() {
        return "Yagee's amazing OpenDocument Text Filter";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.plugins.TextFilterPlugin#getInfo()
     */
    public String getInfo() {
        return "This filter extracts the text out of a OpenDocument Text Document";
    }

    /**
     * @see org.mycore.services.plugins.TextFilterPlugin#getMajorNumber()
     */
    public int getMajorNumber() {
        return MAJOR;
    }

    /**
     * @see org.mycore.services.plugins.TextFilterPlugin#getMinorNumber()
     */
    public int getMinorNumber() {
        return MINOR;
    }

    String getTextNameSpace() {
        return "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
    }

    String getDocumentName() {
        return "OpenDocument Text Document";
    }
}
