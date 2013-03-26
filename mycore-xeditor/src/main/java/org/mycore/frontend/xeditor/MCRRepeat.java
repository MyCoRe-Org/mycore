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

package org.mycore.frontend.xeditor;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.jdom2.JDOMException;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRRepeat {

    private MCRBinding parentBinding;

    private String xPath;

    private int repeatPosition;

    private int numRepeats;

    private int maxRepeats;

    public MCRRepeat(MCRBinding parentBinding, String xPath, int minRepeats, int maxRepeats) throws JDOMException, ParseException {
        this.parentBinding = parentBinding;
        this.xPath = xPath;
        int numBoundNodes = new MCRBinding(xPath, parentBinding).getBoundNodes().size();
        this.numRepeats = Math.max(numBoundNodes, Math.max(minRepeats, 1));
        this.maxRepeats = maxRepeats < 1 ? Integer.MAX_VALUE : maxRepeats;
        this.maxRepeats = Math.max(this.maxRepeats, this.numRepeats);
    }

    public MCRBinding getParentBinding() {
        return parentBinding;
    }

    private String getRepeatPositionXPath() {
        return xPath + "[" + repeatPosition + "]";
    }

    public int getRepeatPosition() {
        return repeatPosition;
    }

    public MCRBinding bindRepeatPosition() throws JDOMException, ParseException {
        repeatPosition++;
        return new MCRBinding(getRepeatPositionXPath(), parentBinding);
    }

    public int getNumRepeats() {
        return numRepeats;
    }

    public int getMaxRepeats() {
        return maxRepeats;
    }

    public String getControlsParameter() throws UnsupportedEncodingException {
        return parentBinding.getAbsoluteXPath() + "_" + MCRRepeat.encode(xPath) + "_" + repeatPosition;
    }

    public static String encode(String text) throws UnsupportedEncodingException {
        return Hex.encodeHexString(text.getBytes("UTF-8"));
    }

    public static String decode(String text) throws DecoderException {
        return new String(Hex.decodeHex(text.toCharArray()));
    }
}
