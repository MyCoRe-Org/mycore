/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import java.io.OutputStream;

import org.mycore.common.content.MCRContent;
import org.mycore.common.xsl.MCRParameterCollector;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public abstract class MCRParameterizedTransformer extends MCRContentTransformer {
    /** Transforms MCRContent. Subclasses implement different transformation methods */
    public abstract MCRContent transform(MCRContent source, MCRParameterCollector parameter) throws IOException;

    public void transform(MCRContent source, OutputStream out, MCRParameterCollector parameter) throws IOException {
        transform(source, parameter).sendTo(out);
    }
}
