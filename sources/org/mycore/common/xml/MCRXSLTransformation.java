/**
 * $RCSfile: MCRXSLTransformation.java,v $
 * $Revision: 1.0 $ $Date: 2003/02/03 14:57:25 $
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

package org.mycore.common.xml;

import java.io.File;

import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.jdom.Document;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;

/**
 * This class implements XSLTransformation functions to be used in all other
 * MyCoRe packages.
 *
 * @author Werner Gresshoff
 *
 * @version $Revision: 1.0 $ $Date: 2003/02/03 14:57:25 $
 **/
public class MCRXSLTransformation {

	/**
	 * Method transform. Transforms a JDOM-Document <i>in</i> with a given <i>stylesheet</i> to a new document.
	 * @param in A JDOM-Document.
	 * @param stylesheet The Filename with complete path (this is not a servlet!) of the stylesheet.
	 * @return Document The new document or null, if an exception was thrown.
	 */
    public static org.jdom.Document transform(org.jdom.Document in, String stylesheet) {
        try {
            JDOMResult out = new JDOMResult();
            Transformer transformer = TransformerFactory.newInstance().newTransformer(
                    new StreamSource(new File(stylesheet)));
            transformer.transform(new JDOMSource(in), out);
            
            return out.getDocument();
        }
        catch (TransformerException e) {
            //logger.fatal(e.getMessage());
            return null;
        }
    }

}
