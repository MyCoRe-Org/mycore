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

package org.mycore.backend.jpa;

import org.mycore.common.MCRPersistenceTransaction;
import org.mycore.common.config.MCRConfiguration2;

public class MCREntityTransaction implements MCRPersistenceTransaction {

    @Override
    public boolean isReady() {
        return MCREntityManagerProvider.getEntityManagerFactory() != null &&
            MCRConfiguration2.getBoolean("MCR.Persistence.Database.Enable").orElse(true);
    }

    @Override
    public void begin() {
        MCREntityManagerProvider.getCurrentEntityManager().getTransaction().begin();
    }

    @Override
    public void commit() {
        MCREntityManagerProvider.getCurrentEntityManager().getTransaction().commit();
    }

    @Override
    public void rollback() {
        MCREntityManagerProvider.getCurrentEntityManager().getTransaction().rollback();
    }

    @Override
    public int getCommitPriority() {
        return 4000;
    }

}
