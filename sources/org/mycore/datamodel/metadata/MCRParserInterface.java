/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package mycore.datamodel;

import org.w3c.dom.Document;
import mycore.common.MCRException;

/**
 * This interface is designed to choose the XML parser. To construct a
 * DOM you have to methodes, one for a URI input an one for a XML stream.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public interface MCRParserInterface
{

/**
 * This method parse the XML file from a URI and returns a DOM.
 *
 * @param uri			the URI of the XML stream
 * @exception MCRException      general Exception of MyCoRe
 * @return			a document object (DOM)
 **/
public Document parseURI(String uri) throws MCRException; 

/**
 * This method parse the XML data stream and returns a DOM.
 *
 * @param xml			the XML data stream
 * @exception MCRException      general Exception of MyCoRe
 * @return			a document object (DOM)
 **/
public Document parseXML(String xml) throws MCRException;

}

