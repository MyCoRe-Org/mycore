/*
 * $Revision$ 
 * $Date$
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

package org.mycore.common.content.streams;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A FilterInputStream that wraps any InputStream to prevent close() is called.
 * Sometimes, third party code or code you can not change closes an InputStream after
 * it returned -1 on read(). But when using ZipInputStream, this is unwanted, because
 * a ZipInputStream may contain other entries that have to be read and invoked code
 * must be prevented from calling close(). With this class, a ZipInputStream or any other
 * stream can be wrapped to regain control of closing.      
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRNotClosingInputStream extends FilterInputStream {

    public MCRNotClosingInputStream(InputStream in) {
        super(in);
    }

    /** 
     * Does nothing. When you want to really close the stream, call reallyClose().
     */
    public void close() throws IOException {
    }

    /**
     * Really closes the stream. 
     */
    public void reallyClose() throws IOException {
        super.close();
    }
}
