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

package org.mycore.restapi.v1;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import org.mycore.frontend.jersey.feature.MCRJerseyDefaultFeature;

/**
 * Jersey configuration 
 * @author Matthias Eichner
 * 
 * @see MCRJerseyDefaultFeature
 * 
 * @version $Revision: $ $Date: $
 * 
 */
@Provider
public class MCRRestFeature extends MCRJerseyDefaultFeature {

    @Override
    protected List<String> getPackages() {
        return Collections.singletonList("org.mycore.restapi.v1");
    }

    @Override
    protected void registerSessionHookFilter(FeatureContext context) {
        // don't register session hook
    }

    @Override
    protected void registerTransactionFilter(FeatureContext context) {
        // don't register transaction filter
    }

}
