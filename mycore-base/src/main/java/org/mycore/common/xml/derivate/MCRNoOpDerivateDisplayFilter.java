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

package org.mycore.common.xml.derivate;

import org.mycore.datamodel.metadata.MCRDerivate;

/**
 * A {@link MCRNoOpDerivateDisplayFilter} is a {@link MCRDerivateDisplayFilter} that, regardless of intent,
 * and always returns <code>null</code>.
 * <p>
 * No configuration options are available.
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.common.xml.derivate.MCRNoOpDerivateDisplayFilter
 * </code></pre>
 */
public class MCRNoOpDerivateDisplayFilter implements MCRDerivateDisplayFilter {

    @Override
    public Boolean isDisplayEnabled(MCRDerivate derivate, String intent) {
        return null;
    }

}
