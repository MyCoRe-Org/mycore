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

package org.mycore.backend.hibernate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.mycore.backend.jpa.MCRStreamQuery;

/**
 * @author Thomas Scheffler (yagee)
 * @param <T> result type of query
 *
 */
public class MCRHibernateQueryStream<T> extends MCRStreamQuery<T> {
    private final Session session;

    private final String jql;

    private final Class<T> type;

    private final Map<String, Object> parameters = new HashMap<>();

    private int fetchSize = -1;

    private int maxResults = -1;

    public MCRHibernateQueryStream(EntityManager em, String jql, Class<T> resultType) {
        this.jql = jql;
        this.type = resultType;
        this.session = em.unwrap(Session.class);
    }

    @Override
    public Stream<T> getResultStream() {
        Query<T> query = session.createQuery(jql, type);
        if (fetchSize >= 0) {
            query.setFetchSize(fetchSize);
        }
        if (maxResults >= 0) {
            query.setMaxResults(maxResults);
        }
        query.setReadOnly(true);
        parameters.entrySet().stream().forEach(e -> query.setParameter(e.getKey(), e.getValue()));
        return query.stream();
    }

    @Override
    public MCRStreamQuery<T> setParameter(String name, Object value) {
        parameters.put(name, value);
        return this;
    }

    @Override
    public MCRStreamQuery<T> setFetchSize(int size) {
        this.fetchSize = size;
        return this;
    }

    @Override
    public MCRStreamQuery<T> setMaxResults(int size) {
        this.maxResults = size;
        return this;
    }

}
