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

package org.mycore.frontend.xeditor.target;

import org.jaxen.JaxenException;
import org.mycore.frontend.xeditor.MCRRepeatBinding;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRSwapTarget extends MCRSwapInsertTarget {

    public static final boolean MOVE_DOWN = true;

    public static final boolean MOVE_UP = false;

    @Override
    protected void handle(int pos, MCRRepeatBinding repeatBinding) {
        repeatBinding.swap(pos);
    }

    public static String getSwapParameter(MCRRepeatBinding repeatBinding, boolean direction) throws JaxenException {
        int pos = repeatBinding.getRepeatPosition() + (direction == MOVE_UP ? -1 : 0);
        return buildParameter(repeatBinding, pos);
    }
}
