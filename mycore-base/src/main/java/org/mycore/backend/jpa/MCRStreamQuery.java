/**
 * 
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

    abstract public Stream<T> getResultStream();

    abstract public MCRStreamQuery<T> setParameter(String name, Object value);

    abstract public MCRStreamQuery<T> setFetchSize(int size);

    abstract public MCRStreamQuery<T> setMaxResults(int size);

    public static <T> MCRStreamQuery<T> getInstance(EntityManager em, String jql, Class<T> resultType) {
        return new MCRHibernateQueryStream<>(em, jql, resultType);
    }

}
