/*
 * $Revision: 27994 $ 
 * $Date: 2013-09-27 09:00:49 +0200 (Fr, 27 Sep 2013) $
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
