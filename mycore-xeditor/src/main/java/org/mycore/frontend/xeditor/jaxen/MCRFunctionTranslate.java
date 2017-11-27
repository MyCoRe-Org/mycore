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
