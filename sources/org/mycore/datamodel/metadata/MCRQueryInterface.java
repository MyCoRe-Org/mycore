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

import java.util.*;
import mycore.common.MCRException;

/**
 * This interface is designed to choose the tranformer from XQuery to
 * the used query system of the persistence layer. 
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public interface MCRQueryInterface
{

/**
 * This method parse the XQuery string and return a vector of MCRObjectID's.
 *
 * @param query	                the XQuery string
 * @param maxresult             the maximum of results
 * @param type                  the MCRObject type
 * @exception MCRException      general Exception of MyCoRe
 * @return			list of MCRObjectID's as a vector
 **/
public Vector getResultList(String query, String type, int maxresult) 
  throws MCRException; 

/**
 * The methode returns the search string for a XML text field for
 * the IBM Content Manager 7 persistence system.a<p>
 * A full XML tag element shows like<br>
 * &lt;subtag sattrib="svalue"&gt;<br>
 * &lt;innertag iattrib="ivalue"&gt;<br>
 * text<br>
 * &lt;/innertag&gt;<br>
 * &lt;/subtag&gt;
 *
 * @param part               the global part of the elements like 'metadata'
 *                           or 'service'
 * @param subtag             the tagname of an element from the list in a tag
 * @param sattrib            the optional attribute vector of a subtag
 * @param svalue             the optional value vector of sattrib
 * @param innertag           the optional inner tag of a subtag element
 * @param iattrib            the optional attribute vector of a innertag
 * @param ivalue             the optional value vector of iattrib
 * @param text               the text value of this element
 * @return the search string for the CM7 text search engine
 **/
public String createSearchStringText(String part, String subtag, 
  String [] sattrib, String [] svalue, String innertag, String [] iattrib, 
  String [] ivalue, String text);

/**
 * The method returns the search string for a XML date field for
 * the IBM Content Manager 7 persistence system.<p>
 * A full XML tag element shows like<br>
 * &lt;subtag sattrib="svalue" ... &gt;<br>
 * date<br>
 * &lt;/subtag&gt;
 *
 * @param part               the global part of the elements like 'metadata'
 *                           or 'service'
 * @param subtag             the tagname of an element from the list in a tag
 * @param sattrib            the optional attribute vector of a subtag
 * @param svalue             the optional value vector of sattrib
 * @param date               the date value of this element
 * @return the search string for the CM7 text search engine
 **/
public String createSearchStringDate(String part, String subtag,
  String [] sattrib, String [] svalue, GregorianCalendar date);

}

