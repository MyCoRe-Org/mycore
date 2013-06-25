/*
 * $Id$
 * $Revision: 5697 $ $Date: Oct 19, 2012 $
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

package org.mycore.backend.hibernate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.DatabaseSequenceFilter;
import org.dbunit.database.QueryDataSet;
import org.dbunit.database.search.TablesDependencyHelper;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.datatype.DefaultDataTypeFactory;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.dataset.filter.ITableFilter;
import org.dbunit.dataset.stream.IDataSetProducer;
import org.dbunit.dataset.stream.StreamingDataSet;
import org.dbunit.dataset.xml.FlatDtdDataSet;
import org.dbunit.dataset.xml.FlatXmlProducer;
import org.dbunit.dataset.xml.FlatXmlWriter;
import org.dbunit.ext.db2.Db2DataTypeFactory;
import org.dbunit.ext.h2.H2DataTypeFactory;
import org.dbunit.ext.hsqldb.HsqldbDataTypeFactory;
import org.dbunit.ext.mssql.MsSqlDataTypeFactory;
import org.dbunit.ext.mysql.MySqlDataTypeFactory;
import org.dbunit.ext.oracle.Oracle10DataTypeFactory;
import org.dbunit.ext.oracle.OracleDataTypeFactory;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.dbunit.operation.TransactionOperation;
import org.dbunit.util.search.SearchException;
import org.hibernate.HibernateException;
import org.hibernate.StatelessSession;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.service.jdbc.dialect.spi.DialectFactory;
import org.xml.sax.InputSource;

/**
 * @author Thomas Scheffler (yagee)
 * 
 */
class MCRHIBImpExTools {
    private static final Logger LOGGER = Logger.getLogger(MCRHIBImpExTools.class.getName());

    public static void exportDatabase(File outputFile, String... rootTables) throws HibernateException,
        DatabaseUnitException, SQLException, IOException {
        DatabaseConnection jdbcConnection = getDatabaseConnection();
        LOGGER.info("Present tables: " + getAvailableTables(jdbcConnection));
        try {
            IDataSet exportSet = getExportDataSet(jdbcConnection, rootTables);
            exportDataSet(outputFile, exportSet);
        } finally {
            jdbcConnection.close();
        }
    }

    private static List<String> getAvailableTables(DatabaseConnection jdbcConnection) throws SQLException,
        DataSetException {
        IDataSet dataSet = jdbcConnection.createDataSet();
        return Arrays.asList(dataSet.getTableNames());
    }

    private static DatabaseConnection getDatabaseConnection() throws DatabaseUnitException, SQLException {
        MCRHIBConnection mcrhibConnection = MCRHIBConnection.instance();
        int batchSize = Integer.parseInt(
            mcrhibConnection.getConfiguration().getProperties().getProperty("jdbc.batch_size", "0"), 10);
        StatelessSession session = mcrhibConnection.getSessionFactory().openStatelessSession();
        Connection jdbcConnection = session.connection();
        DatabaseMetaData metaData = jdbcConnection.getMetaData();
        DatabaseConnection databaseConnection = new DatabaseConnection(jdbcConnection);
        DatabaseConfig databaseConfig = databaseConnection.getConfig();
        databaseConfig.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, getDataTypeFactory());
        if (metaData.supportsBatchUpdates() && batchSize > 1) {
            LOGGER.info("JDBC Batch support detected. Batch size: " + batchSize);
            databaseConfig.setProperty(DatabaseConfig.FEATURE_BATCHED_STATEMENTS, true);
            databaseConfig.setProperty(DatabaseConfig.PROPERTY_BATCH_SIZE, batchSize);
        } else {
            databaseConfig.setProperty(DatabaseConfig.FEATURE_BATCHED_STATEMENTS, false);
        }
        databaseConfig.setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, false);
        return databaseConnection;
    }

    public static void importDatabase(File inputFileName) throws DatabaseUnitException, SQLException {
        File dtdFile = getDTDFile(inputFileName);
        boolean dtdMetadata = dtdFile.exists();
        InputSource xmlSource = new InputSource(inputFileName.toURI().toString());
        IDataSetProducer producer = new FlatXmlProducer(xmlSource, dtdMetadata);
        IDataSet dataSet = new StreamingDataSet(producer);
        DatabaseConnection databaseConnection = getDatabaseConnection();
        try {
            Connection jdbcConnection = databaseConnection.getConnection();
            DatabaseMetaData metaData = jdbcConnection.getMetaData();
            metaData.supportsTransactions();
            DatabaseOperation operation;
            if (metaData.supportsTransactions()) {
                LOGGER.info(metaData.getDatabaseProductName() + " v" + metaData.getDatabaseProductVersion()
                    + " does not support transactions.");
                operation = DatabaseOperation.INSERT;
            } else {
                LOGGER.info("Importing with transaction support");
                operation = new TransactionOperation(DatabaseOperation.INSERT);
            }
            operation.execute(databaseConnection, dataSet);
        } finally {
            databaseConnection.close();
        }
    }

    static IDataTypeFactory getDataTypeFactory() {
        Properties properties = MCRHIBConnection.instance().getConfiguration().getProperties();
        Dialect dialect = MCRHIBConnection.instance().getServiceRegistry().getService(DialectFactory.class)
            .buildDialect(properties, null);
        if (dialect == null) {
            LOGGER.info("Could not detect Hibernate dialect: " + properties);
            return new DefaultDataTypeFactory();
        }
        if (dialect instanceof HSQLDialect) {
            LOGGER.info("HSQLDB detected.");
            return new HsqldbDataTypeFactory();
        }
        if (dialect instanceof DB2Dialect) {
            LOGGER.info("DB2 detected.");
            return new Db2DataTypeFactory();
        }
        if (dialect instanceof MySQLDialect) {
            LOGGER.info("MySQL detected.");
            return new MySqlDataTypeFactory();
        }
        if (dialect instanceof PostgreSQL81Dialect || dialect instanceof PostgreSQL82Dialect) {
            LOGGER.info("PostgreSQL detected.");
            return new PostgresqlDataTypeFactory();
        }
        if (dialect instanceof H2Dialect) {
            LOGGER.info("H2 detected.");
            return new H2DataTypeFactory();
        }
        if (dialect instanceof SQLServerDialect) {
            LOGGER.info("MS SQLServer detected.");
            return new MsSqlDataTypeFactory();
        }
        if (dialect instanceof Oracle10gDialect) {
            LOGGER.info("Oracle 10 detected.");
            return new Oracle10DataTypeFactory();
        }
        if (dialect instanceof Oracle8iDialect) {
            LOGGER.info("Oracle <10 detected.");
            return new OracleDataTypeFactory();
        }
        LOGGER.info("Unsupported Hibernate dialect: " + dialect.getClass());
        return new DefaultDataTypeFactory();
    }

    private static IDataSet getExportDataSet(DatabaseConnection jdbcConnection, String... rootTables)
        throws SQLException, SearchException, DataSetException {
        ITableFilter filter = new DatabaseSequenceFilter(jdbcConnection);
        IDataSet dataSet;
        if (rootTables.length == 0) {
            dataSet = jdbcConnection.createDataSet();
        } else {
            String[] depTableNames = TablesDependencyHelper.getAllDependentTables(jdbcConnection, rootTables);
            dataSet = jdbcConnection.createDataSet(depTableNames);
        }
        return getExportableOrder(jdbcConnection, filter, dataSet);
    }

    static IDataSet getExportableOrder(DatabaseConnection jdbcConnection, ITableFilter filter, IDataSet dataSet)
        throws DataSetException {
        FilteredDataSet baseSet = new FilteredDataSet(filter, dataSet);
        String[] tableNames = baseSet.getTableNames();
        QueryDataSet qDataSet = new QueryDataSet(jdbcConnection);
        for (String tableName : tableNames) {
            qDataSet.addTable(tableName, getQuery(tableName));
        }
        return qDataSet;
    }

    private static String getQuery(String tableName) {
        String uCaseName = tableName.toUpperCase();
        if (uCaseName.endsWith("MCRCATEGORY")) {
            return MessageFormat.format("select * from {0} order by level asc", tableName);
        }
        return null;
    }

    static void exportDataSet(File outputFile, IDataSet exportSet) throws FileNotFoundException, IOException,
        DataSetException {
        File dtdFile = getDTDFile(outputFile);
        FileOutputStream dout = new FileOutputStream(dtdFile);
        try {
            LOGGER.info("Writing DTD to " + dtdFile.getAbsolutePath());
            FlatDtdDataSet.write(exportSet, dout);
        } finally {
            dout.close();
        }
        FileOutputStream fout = new FileOutputStream(outputFile);
        try {
            LOGGER.info("Writing XML to " + outputFile.getAbsolutePath());
            FlatXmlWriter datasetWriter = new FlatXmlWriter(fout);
            datasetWriter.setDocType(dtdFile.getName());
            datasetWriter.write(exportSet);
        } finally {
            fout.close();
        }
    }

    private static File getDTDFile(File outputFile) {
        String dtdFileName = outputFile.getName();
        int pos = dtdFileName.lastIndexOf(".");
        if (pos > 0) {
            dtdFileName = dtdFileName.substring(0, pos);
        }
        dtdFileName += ".dtd";
        File dtdFile = new File(outputFile.getParent(), dtdFileName);
        return dtdFile;
    }

}
