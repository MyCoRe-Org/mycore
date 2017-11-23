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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaxen.Context;
import org.jaxen.FunctionCallException;

class MCRFunctionGenerateID implements org.jaxen.Function {

    private static final Logger LOGGER = LogManager.getLogger(MCRFunctionGenerateID.class);

    @Override
    public Object call(Context context, List args) throws FunctionCallException {
        try {
            Object targetNode = args.isEmpty() ? context.getNodeSet().get(0) : ((List) args.get(0)).get(0);
            return "n" + System.identityHashCode(targetNode);
        } catch (Exception ex) {
            LOGGER.warn("Exception in call to generate-id", ex);
            return ex.getMessage();
        }
    }
}
