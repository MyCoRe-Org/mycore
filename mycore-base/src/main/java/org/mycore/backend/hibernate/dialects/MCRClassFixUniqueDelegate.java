/**
 * 
 */
package org.mycore.backend.hibernate.dialects;

import java.util.Locale;
import java.util.Optional;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.unique.DefaultUniqueDelegate;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.UniqueKey;

/**
 * Makes unique checks on MCRCategory that uses "leftValue" and "rightValue" constraints deferrable.

 * @author Thomas Scheffler (yagee)
 */
public class MCRClassFixUniqueDelegate extends DefaultUniqueDelegate {

    public MCRClassFixUniqueDelegate(Dialect dialect) {
        super(dialect);
    }

    @Override
    protected String uniqueConstraintSql(UniqueKey uniqueKey) {
        if (requiresDeferrable(uniqueKey)) {
            return super.uniqueConstraintSql(uniqueKey) + " DEFERRABLE INITIALLY DEFERRED";
        }
        return super.uniqueConstraintSql(uniqueKey);
    }

    private boolean requiresDeferrable(UniqueKey uniqueKey) {
        return Optional.of(uniqueKey)
            .filter(this::isInTable)
            .filter(this::hasColumns)
            .isPresent();
    }

    private boolean isInTable(UniqueKey uniqueKey) {
        return uniqueKey.getTable().getName().toLowerCase(Locale.ROOT).equals("mcrcategory");
    }

    private boolean hasColumns(UniqueKey uniqueKey) {
        return uniqueKey.getColumns().stream().filter(this::isColumn).findAny().isPresent();
    }

    private boolean isColumn(Column column) {
        String cName = column.getName().toLowerCase(Locale.ROOT);
        return cName.equals("leftvalue") || cName.equals("rightvalue");
    }

}
