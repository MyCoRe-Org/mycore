/*
 * $Revision: 21452 $ 
 * $Date: 2011-07-13 10:39:39 +0200 (Mi, 13 Jul 2011) $
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
package org.mycore.buildtools.common;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
/**
 * This class enhances the Java Properties class.
 * If the properties are stored, they will be sorted by key names.
 * 
 * @see java.util.Properties
 * 
 * @author R. Adler
 */
public class SortedProperties extends Properties {
 	private static final long serialVersionUID = 1L;

	/**
     * Overrides, called by the store method.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public synchronized Enumeration keys() {
       Enumeration keysEnum = super.keys();
       Vector keyList = new Vector();
       while(keysEnum.hasMoreElements()){
         keyList.add(keysEnum.nextElement());
       }
       Collections.sort(keyList);
       return keyList.elements();
    }
}