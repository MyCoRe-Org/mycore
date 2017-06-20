/**
 * $Revision: 1.8 $ $Date: 2008/05/28 13:43:31 $
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

package org.mycore.common.fo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.TransformerException;

import org.mycore.common.content.MCRContent;

/**
 * This is an interface to use configured XSL-FO formatters for the layout service.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision: 1.8 $ $Date: 2008/05/28 13:43:31 $
 */

public interface MCRFoFormatterInterface {

    /**
     * @deprecated use {@link #transform(MCRContent, OutputStream)}
     */
    @Deprecated
    public void transform(InputStream fo_stream, OutputStream out) throws TransformerException, IOException;

    default public void transform(MCRContent input, OutputStream out) throws TransformerException, IOException {
        transform(input.getInputStream(), out);
    }
}
