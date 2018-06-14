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

package org.mycore.restapi.converter;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRContent;

@Provider
@Produces({ MediaType.APPLICATION_JSON + ";charset=UTF-8" })
public class MCRContentJSONWriter extends MCRContentAbstractWriter {

    @Override
    protected MediaType getTransfomerFormat() {
        return MediaType.APPLICATION_JSON_TYPE;
    }

    @Override
    protected void handleFallback(MCRContent content, OutputStream entityStream) throws IOException {
        String msg = "No Transformer defined to transform MCRContent to JSON";
        if (content.getSystemId() != null) {
            msg = "No Transformer defined to transform MCRContent " + content.getSystemId() + " to JSON";
        }
        throw new MCRConfigurationException(msg);
    }

}
