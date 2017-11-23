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

import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.mycore.backend.hibernate.MCRHibernateQueryStream;

/**
 * Provides a read-only Stream of results.
 * @author Thomas Scheffler (yagee)
 *
 */
public abstract class MCRStreamQuery<T> {

    public abstract Stream<T> getResultStream();

    public abstract MCRStreamQuery<T> setParameter(String name, Object value);

    public abstract MCRStreamQuery<T> setFetchSize(int size);

    public abstract MCRStreamQuery<T> setMaxResults(int size);

    public static <T> MCRStreamQuery<T> getInstance(EntityManager em, String jql, Class<T> resultType) {
        return new MCRHibernateQueryStream<>(em, jql, resultType);
    }

}
