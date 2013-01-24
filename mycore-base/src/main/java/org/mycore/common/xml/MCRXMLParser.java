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

package org.mycore.common.xml;

import org.jdom2.Document;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.xml.sax.SAXParseException;

/**
 * Parses XML content and returns it as JDOM document.
 * 
 * Implementations of this interface are not thread safe.
 * 
 * @author Frank L\u00FCtzenkirchen
 * @author Thomas Scheffler (yagee)
 */
public interface MCRXMLParser {

    public boolean isValidating();

    public Document parseXML(MCRContent content) throws MCRException, SAXParseException;
}
