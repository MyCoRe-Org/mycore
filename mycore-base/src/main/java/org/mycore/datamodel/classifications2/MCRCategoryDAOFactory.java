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

package org.mycore.datamodel.classifications2;

import java.util.Objects;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImpl;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 2.0
 */
public class MCRCategoryDAOFactory {

    /**
     * Returns an instance of a MCRCategoryDAO implementator.
     */
    public static MCRCategoryDAO getInstance() {
        return Objects.requireNonNull(MCRCategoryDAOHolder.INSTANCE,
            "MCRCategory cannot be NULL - There is a problem with the loading order of classes");
    }

    /**
     * Sets a new category dao implementation for this factory. This could be useful for different test cases
     * with mock objects.
     * 
     * @param daoClass new dao class
     */
    public static synchronized void set(Class<? extends MCRCategoryDAO> daoClass) throws ReflectiveOperationException {
        MCRCategoryDAOHolder.INSTANCE = daoClass.getDeclaredConstructor().newInstance();
    }

    // encapsulate the INSTANCE in an inner static class to avoid issues with class loading order
    // this is known as "Bill Pugh singleton"
    private static class MCRCategoryDAOHolder {
        private static MCRCategoryDAO INSTANCE = MCRConfiguration2.<MCRCategoryDAO>getInstanceOf("MCR.Category.DAO")
            .orElseGet(MCRCategoryDAOImpl::new);
    }

}
