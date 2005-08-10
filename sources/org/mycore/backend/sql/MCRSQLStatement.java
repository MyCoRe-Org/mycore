/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.backend.sql;

import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.mycore.common.MCRArgumentChecker;
import org.mycore.common.MCRConfiguration;

/**
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRSQLStatement {
    protected final static String NULL = "NULL";

    protected Properties values;

    protected Properties conditions;

    protected Vector columns;

    protected String tableName;

    private static char MASK_CHAR;

    static {
        MCRConfiguration config = MCRConfiguration.instance();
        if (config.getString("MCR.persistence_sql_database_url").indexOf(
                ":db2:") > 0) {
            MASK_CHAR = '\'';
        } else {
            MASK_CHAR = '\\';
        }

    }

    public MCRSQLStatement(String tableName) {
        this.tableName = tableName;
        this.values = new Properties();
        this.conditions = new Properties();
        this.columns = new Vector();
    }

    public final MCRSQLStatement setValue(String columnName, String columnValue) {
        MCRArgumentChecker.ensureNotEmpty(columnName, "columnName");

        if (columnValue == null)
            values.put(columnName, NULL);
        else
            values.put(columnName, mask(columnValue));

        return this;
    }

    public final MCRSQLStatement setCondition(String columnName,
            String columnValue) {
        MCRArgumentChecker.ensureNotEmpty(columnName, "columnName");

        if (columnValue == null)
            conditions.put(columnName, NULL);
        else
            conditions.put(columnName, mask(columnValue));

        return this;
    }

    public final MCRSQLStatement addColumn(String columnDefinition) {
        MCRArgumentChecker.ensureNotEmpty(columnDefinition, "columnDefinition");
        columns.addElement(columnDefinition);
        return this;
    }

    protected final String getSQLValue(String key) {
        String value = values.getProperty(key);
        return (value == NULL ? "NULL" : "'" + value + "'");
    }

    protected final String condition() {
        if (conditions.isEmpty())
            return "";

        StringBuffer sql = new StringBuffer(" WHERE ");

        Enumeration keys = conditions.keys();
        while (keys.hasMoreElements()) {
            String key = (String) (keys.nextElement());
            String value = conditions.getProperty(key);

            sql.append(key).append(" ");

            if (value == NULL) {
                sql.append("IS NULL");
            } else {
                if (value.indexOf("%") == -1) {
                    sql.append("= '").append(value).append("'");
                } else {
                    sql.append("LIKE '").append(value).append("'");
                }
            }

            if (keys.hasMoreElements())
                sql.append(" AND ");
        }
        return sql.toString();
    }

    public final String toInsertStatement() {
        StringBuffer statement = new StringBuffer("INSERT INTO ");
        statement.append(tableName).append(" (");

        StringBuffer columnList = new StringBuffer();
        StringBuffer valueList = new StringBuffer();

        Enumeration keys = values.keys();
        while (keys.hasMoreElements()) {
            String column = (String) (keys.nextElement());
            String value = getSQLValue(column);

            columnList.append(" ").append(column);
            valueList.append(" ").append(value);

            if (keys.hasMoreElements()) {
                columnList.append(",");
                valueList.append(",");
            }
        }

        statement.append(columnList.toString()).append(" ) VALUES (");
        statement.append(valueList.toString()).append(" )");

        return statement.toString();
    }

    public final String toUpdateStatement() {
        StringBuffer statement = new StringBuffer("UPDATE ");
        statement.append(tableName).append(" SET");

        Enumeration keys = values.keys();
        while (keys.hasMoreElements()) {
            String key = (String) (keys.nextElement());
            String value = getSQLValue(key);

            statement.append(" ").append(key).append(" =");
            statement.append(" ").append(value);
            if (keys.hasMoreElements())
                statement.append(", ");
        }

        statement.append(condition());

        return statement.toString();
    }

    public final String toCreateTableStatement() {
        StringBuffer statement = new StringBuffer("CREATE TABLE ");
        statement.append(tableName).append(" (");

        for (int i = 0; i < columns.size(); i++) {
            statement.append(" ").append(columns.elementAt(i));
            if (i < columns.size() - 1)
                statement.append(",");
        }

        statement.append(" )");

        return statement.toString();
    }

    public final String toIndexStatement() {
        StringBuffer statement = new StringBuffer("CREATE INDEX ");
        statement.append(tableName).append("_INDEX ON ").append(tableName)
                .append(" (");
        for (int i = 0; i < columns.size(); i++) {
            statement.append(" ").append(columns.elementAt(i));
            if (i < columns.size() - 1)
                statement.append(",");
        }
        statement.append(" )");
        return statement.toString();
    }

    public final String toRowSelector() {
        return new StringBuffer(tableName).append(condition()).toString();
    }

    public final String toSelectStatement() {
        return "SELECT * FROM " + toRowSelector();
    }

    public final String toSelectStatement(String columns) {
        MCRArgumentChecker.ensureNotEmpty(columns, "Columns");
        return new StringBuffer("SELECT ").append(columns).append(" FROM ")
                .append(toRowSelector()).toString();
    }

    public final String toDeleteStatement() {
        return "DELETE FROM " + toRowSelector();
    }

    public final String toCountStatement(String column) {
        return "SELECT COUNT( DISTINCT "+column+" ) AS NUMBER FROM "
                + toRowSelector();
    }

    /**
     * masks the character ' in an sql statement
     * 
     * @param value
     *            to be masked
     * @return value with masked '
     */
    private final String mask(String value) {
        final char mask = '\'';
	return value.replaceAll(""+mask, ""+MASK_CHAR+mask);
    }

}
