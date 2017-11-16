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

    public abstract Stream<T> getResultStream();

    public abstract MCRStreamQuery<T> setParameter(String name, Object value);

    public abstract MCRStreamQuery<T> setFetchSize(int size);

    public abstract MCRStreamQuery<T> setMaxResults(int size);

    public static <T> MCRStreamQuery<T> getInstance(EntityManager em, String jql, Class<T> resultType) {
        return new MCRHibernateQueryStream<>(em, jql, resultType);
    }

}
