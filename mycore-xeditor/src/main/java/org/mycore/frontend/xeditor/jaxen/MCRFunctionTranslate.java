/*
 * $Revision: 28699 $ 
 * $Date: 2013-12-19 21:45:48 +0100 (Do, 19 Dez 2013) $
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

package org.mycore.frontend.xeditor.jaxen;

import java.util.List;

import org.jaxen.Context;
import org.jaxen.FunctionCallException;
import org.mycore.services.i18n.MCRTranslation;

/**
 * 
 * @author Frank L\u00FCtzenkirchen
 */
class MCRFunctionTranslate implements org.jaxen.Function {

    @Override
    public Object call(Context context, List args) throws FunctionCallException {
        String i18nkey = (String) (args.get(0));
        if (args.size() == 1)
            return MCRTranslation.translate(i18nkey);
        else
            return MCRTranslation.translate(i18nkey, args.subList(1, args.size()));
    }
}
