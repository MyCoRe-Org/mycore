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

package org.mycore.datamodel.classifications2;

import java.util.Objects;

import org.mycore.common.config.MCRConfiguration2;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * @since 2.0
 */
public class MCRCategoryDAOFactory {

    /**
     * Returns an instance of a MCRCategoryDAO implementation.
     */
    public static MCRCategoryDAO obtainInstance() {
        return Objects.requireNonNull(MCRCategoryDAOHolder.instance,
            "MCRCategoryDAO cannot be NULL - There is a problem with the loading order of classes");
    }

    /**
     * Sets a new category dao implementation for this factory. This could be useful for different test cases
     * with mock objects.
     * 
     * @param daoClass new dao class
     */
    public static synchronized void set(Class<? extends MCRCategoryDAO> daoClass) throws ReflectiveOperationException {
        MCRCategoryDAOHolder.instance = daoClass.getDeclaredConstructor().newInstance();
    }

    // encapsulate the instance in an inner static class to avoid issues with class loading order
    // this is known as "Bill Pugh singleton"
    private static final class MCRCategoryDAOHolder {
        private static MCRCategoryDAO instance = MCRConfiguration2.getInstanceOfOrThrow(
            MCRCategoryDAO.class, "MCR.Category.DAO");
    }

}
