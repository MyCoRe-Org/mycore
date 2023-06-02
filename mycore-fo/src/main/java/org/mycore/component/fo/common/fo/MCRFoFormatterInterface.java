/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.component.fo.common.fo;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.TransformerException;

import org.mycore.common.content.MCRContent;

/**
 * This is an interface to use configured XSL-FO formatters for the layout service.
 * 
 * @author Jens Kupferschmidt
 */
public interface MCRFoFormatterInterface {

    /**
     * transform the given MCRContent to the given OutputStream
     * 
     * @param input the MCRContent
     * @param out the target output stream
     * @throws TransformerException if the transformation goes wrong
     * @throws IOException if an I/O error occurs
     */
    void transform(MCRContent input, OutputStream out) throws TransformerException, IOException;
}
