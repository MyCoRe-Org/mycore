/**
 * $RCSfile$
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
 *
 **/

package org.mycore.common;

/**
 * This class holds only static variables, they ar used in all classes as
 * default values. So we can change this in one source code. The values
 * can be changed be the propery configurations.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public interface MCRDefaults
{

/** The URL of the XLink */
public final static String XLINK_URL = "http://www.w3.org/1999/xlink";

/** The URL of the XSI */
public final static String XSI_URL = "http://www.w3.org/2001/XMLSchema-instance";

/** The default encoding */
public final static String ENCODING = "ISO_8859-1";

}

