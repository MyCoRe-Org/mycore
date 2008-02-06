/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.backend.sql;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRUtils;

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

    protected List sqlColumns;

    protected String tableName;

    private static String MASK_WHAT = "'";

    private static String MASK_WITH;

    static {
        MCRConfiguration config = MCRConfiguration.instance();

        if (config.getString("MCR.persistence_sql_database_url").indexOf(":db2:") > 0) {
            MASK_WITH = "''";
        } else {
            MASK_WITH = "\\'";
        }
    }

    public MCRSQLStatement(String tableName) {
        this.tableName = tableName;
        this.values = new Properties();
        this.conditions = new Properties();
        this.columns = new Vector();
        this.sqlColumns = new LinkedList();
    }

    public final MCRSQLStatement setValue(String columnName, String columnValue) {
        if (columnValue == null) {
            values.put(columnName, NULL);
        } else {
            values.put(columnName, mask(columnValue));
        }

        // new behaviour
        sqlColumns.add(new MCRSQLColumn(columnName, columnValue, "string"));

        return this;
    }

    public final MCRSQLStatement setValue(MCRSQLColumn column) {
        if (column != null) {
            sqlColumns.add(column);
        }

        return this;
    }

    public final MCRSQLStatement setCondition(String columnName, String columnValue) {
        if (columnValue == null) {
            conditions.put(columnName, NULL);
        } else {
            conditions.put(columnName, mask(columnValue));
        }

        return this;
    }

    public final MCRSQLStatement addColumn(String columnDefinition) {
        columns.addElement(columnDefinition);

        return this;
    }

    protected final String getSQLValue(String key) {
        String value = values.getProperty(key);

        return ((value == NULL) ? "NULL" : ("'" + value + "'"));
    }

    protected final String condition() {
        if (conditions.isEmpty()) {
            return "";
        }

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

            if (keys.hasMoreElements()) {
                sql.append(" AND ");
            }
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

        // new behaviour - needs more tests
        // return toTypedInsertStatement();
    }

    public final String toTypedInsertStatement() {
        StringBuffer statement = new StringBuffer("INSERT INTO ");
        statement.append(tableName).append(" (");

        StringBuffer columnList = new StringBuffer();
        StringBuffer valueList = new StringBuffer();

        for (int i = 0; i < sqlColumns.size(); i++) {
            MCRSQLColumn col = (MCRSQLColumn) sqlColumns.get(i);
            String column = col.getName();
            String value = col.getValue();

            if ((value != null) && (value != "null")) {
                if (col.getType().toLowerCase().equals("string")) {
                    value = "'" + value + "'";
                } else if (col.getType().toLowerCase().equals("date") || col.getType().toLowerCase().equals("time") || col.getType().toLowerCase().equals("timestamp")) {
                    // date
                    value = "'" + value + "'";
                } else if (col.getType().toLowerCase().equals("integer")) {
                    // integer
                    try {
                        value = "" + Integer.parseInt(value);
                    } catch (Exception e) {
                        value = "0";
                    }
                } else if (col.getType().toLowerCase().equals("decimal")) {
                    // decimal
                    try {
                        value = "" + Double.parseDouble(value.replaceAll(",", "."));
                    } catch (Exception e) {
                        value = "0";
                    }
                } else if (col.getType().toLowerCase().equals("boolean")) {
                    // boolean
                    if (value.toLowerCase() == "true") {
                        value = "1";
                    } else if (value.toLowerCase() == "false") {
                        value = "0";
                    }
                }

                columnList.append(" ").append("`" + column + "`");
                valueList.append(" ").append(value);

                if (i < (sqlColumns.size() - 1)) {
                    columnList.append(",");
                    valueList.append(",");
                }
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

            if (keys.hasMoreElements()) {
                statement.append(", ");
            }
        }

        statement.append(condition());

        return statement.toString();
    }

    public final String toCreateTableStatement() {
        StringBuffer statement = new StringBuffer("CREATE TABLE ");
        statement.append(tableName).append(" (");

        for (int i = 0; i < columns.size(); i++) {
            statement.append(" ").append(columns.elementAt(i));

            if (i < (columns.size() - 1)) {
                statement.append(",");
            }
        }

        statement.append(" )");

        return statement.toString();
    }

    public final String toIndexStatement() {
        StringBuffer statement = new StringBuffer("CREATE INDEX ");
        statement.append(tableName).append("_INDEX ON ").append(tableName).append(" (");

        for (int i = 0; i < columns.size(); i++) {
            statement.append(" ").append(columns.elementAt(i));

            if (i < (columns.size() - 1)) {
                statement.append(",");
            }
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
        return new StringBuffer("SELECT ").append(columns).append(" FROM ").append(toRowSelector()).toString();
    }

    public final String toDeleteStatement() {
        return "DELETE FROM " + toRowSelector();
    }

    public final String toCountStatement(String column) {
        return "SELECT COUNT( DISTINCT " + column + " ) AS NUMBER FROM " + toRowSelector();
    }

    /**
     * masks the character ' in an sql statement
     * 
     * @param value
     *            to be masked
     * @return value with masked '
     */
    private final String mask(String value) {
        if (value.indexOf(MASK_WHAT) >= 0){
            return MCRUtils.replaceString(value, MASK_WHAT, MASK_WITH);
        }
        return value;
    }
}
