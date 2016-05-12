/**
 * 
 */
package org.mycore.backend.hibernate.dialects;

import org.hibernate.dialect.PostgreSQL9Dialect;
import org.hibernate.dialect.unique.UniqueDelegate;

/**
 * Fixes unique constraints for Classifications.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRPostgreSQL9Dialect extends PostgreSQL9Dialect {

    private UniqueDelegate uniqueDelegate;

    public MCRPostgreSQL9Dialect() {
        super();
        this.uniqueDelegate = new MCRClassFixUniqueDelegate(this);
    }

    @Override
    public UniqueDelegate getUniqueDelegate() {
        return uniqueDelegate;
    }

}
