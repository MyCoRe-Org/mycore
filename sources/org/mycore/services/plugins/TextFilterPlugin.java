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
package org.mycore.services.plugins;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;

/**
 * The Plugin spec for filtering several documents for the fulltext search.
 * 
 * @author Thomas Scheffler (yagee)
 */
public interface TextFilterPlugin {
	
	/**
	 * should return a Name of the plugin
	 * @return Plugin name
	 */
	public String getName();
	
	/**
	 * may contain some additional Information on the plugin
	 * @return further Informations on the plugin
	 */
	public String getInfo();

	/**
	 * returns a list of all supported file extensions.
	 * 
	 * These file extensions must be delivered without the leading dot.
	 * 
	 * @return HashSet List of file extensions
 	 */
	public HashSet getSupportedExtensions();


	/**
	 * returns a list of all supported MIME Types.
	 * 
	 * They should be like "text/plain".
	 * 
	 * @return HashSet List of MIME types
	 */
	public HashSet getSupportedMimeTypes();

	/**
	 * converts a given Inputstream to Textstream which should contain
	 * a textual representation of the source.
	 * 
	 * @param input	Inputstream in foreign format
 	 * @param mime   MIME Type of InputStream
	 * @return  InputStream textual representation of input
	 */
	public boolean transform(InputStream input, String mime, OutputStream output) throws FilterPluginTransformException;

	/**
	 * onverts a given Inputstream to Textstream which should contain
	 * a textual representation of the source.
	 * 
	 * @param input  File in foreign format
	 * @return Inputstream textual representation of input
	 */
	public boolean transform(File input, OutputStream output) throws FilterPluginTransformException;
}
