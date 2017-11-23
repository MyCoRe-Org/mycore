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

package org.mycore.backend.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

public class MCREntityManagerProvider {

    private static EntityManagerFactory factory;

    private static MCRSessionContext context;

    private static PersistenceException initException;

    public static EntityManagerFactory getEntityManagerFactory() {
        return factory;
    }

    public static EntityManager getCurrentEntityManager() {
        if (context == null && initException != null) {
            throw initException;
        }
        return context.getCurrentEntityManager();
    }

    static void init(EntityManagerFactory factory) {
        MCREntityManagerProvider.factory = factory;
        context = new MCRSessionContext(factory);
    }

    static void init(PersistenceException e) {
        initException = e;
    }

}
