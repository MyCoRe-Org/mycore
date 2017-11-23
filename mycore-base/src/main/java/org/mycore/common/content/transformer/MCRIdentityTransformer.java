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

package org.mycore.common.content.transformer;

import java.io.IOException;

import org.mycore.common.content.MCRContent;

/**
 * This is a noop transformer.
 * 
 * The {@link #transform(MCRContent)} method just returns the instance given as the parameter.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRIdentityTransformer extends MCRContentTransformer {

    public MCRIdentityTransformer(String mimeType, String fileExtension) {
        this.mimeType = mimeType;
        this.fileExtension = fileExtension;
    }

    /* (non-Javadoc)
     * @see org.mycore.common.content.transformer.MCRContentTransformer#transform(org.mycore.common.content.MCRContent)
     */
    @Override
    public MCRContent transform(MCRContent source) throws IOException {
        return source;
    }

}
