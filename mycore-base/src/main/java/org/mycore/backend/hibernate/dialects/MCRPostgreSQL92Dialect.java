/**
 * 
 */
package org.mycore.backend.hibernate.dialects;

import org.hibernate.dialect.PostgreSQL92Dialect;
import org.hibernate.dialect.unique.UniqueDelegate;

/**
 * Fixes unique constraints for Classifications.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRPostgreSQL92Dialect extends PostgreSQL92Dialect {

    private UniqueDelegate uniqueDelegate;

    public MCRPostgreSQL92Dialect() {
        super();
        this.uniqueDelegate = new MCRClassFixUniqueDelegate(this);
    }

    @Override
    public UniqueDelegate getUniqueDelegate() {
        return uniqueDelegate;
    }

}
