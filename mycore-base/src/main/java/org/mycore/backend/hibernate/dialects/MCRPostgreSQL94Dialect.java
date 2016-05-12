/**
 * 
 */
package org.mycore.backend.hibernate.dialects;

import org.hibernate.dialect.PostgreSQL94Dialect;
import org.hibernate.dialect.unique.UniqueDelegate;

/**
 * Fixes unique constraints for Classifications.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRPostgreSQL94Dialect extends PostgreSQL94Dialect {

    private UniqueDelegate uniqueDelegate;

    public MCRPostgreSQL94Dialect() {
        super();
        this.uniqueDelegate = new MCRClassFixUniqueDelegate(this);
    }

    @Override
    public UniqueDelegate getUniqueDelegate() {
        return uniqueDelegate;
    }

}
