/*
 * $RCSfile$
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

package org.mycore.backend.hibernate;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import org.mycore.backend.hibernate.tables.MCRACCESS;
import org.mycore.backend.hibernate.tables.MCRACCESSRULE;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;

/**
 * This class provides a set of commands for the org.mycore.access package which
 * can be used by the command line interface. (creates sql tables, run queries
 * 
 * @author Thomas Scheffler (yagee)
 * @author Arne Seifert
 */
public class MCRHIBCtrlCommands extends MCRAbstractCommands {
    /** The logger */
    private static final Logger LOGGER = Logger.getLogger(MCRHIBCtrlCommands.class.getName());

    private static final DocumentFactory DOC_FACTORY = new DocumentFactory();

    /**
     * Pattern that matches against every element directly under the root element.
     */
    private static final java.util.regex.Pattern OBJECT_PATTERN = Pattern.compile("^/[^/]+/[^/]+$");

    /**
     * constructor with commands.
     */
    public MCRHIBCtrlCommands() {
        super();

        MCRCommand com = null;

        com = new MCRCommand("init hibernate", "org.mycore.backend.hibernate.MCRHIBCtrlCommands.createTables",
                "The command creates all tables for MyCoRe by hibernate.");
        command.add(com);
        com = new MCRCommand("export acl rules to file {0}", "org.mycore.backend.hibernate.MCRHIBCtrlCommands.exportAccessRules String",
                "Exports all ACL rules to a given file.");
        command.add(com);
        com = new MCRCommand("import acl rules from file {0}", "org.mycore.backend.hibernate.MCRHIBCtrlCommands.importAccessRules String",
                "Imports all ACL rules from a given file.");
        command.add(com);
        com = new MCRCommand("export acl mappings to file {0}", "org.mycore.backend.hibernate.MCRHIBCtrlCommands.exportAccessMappings String",
                "Exports all ACL mappings to a given file.");
        command.add(com);
        com = new MCRCommand("import acl mappings from file {0}", "org.mycore.backend.hibernate.MCRHIBCtrlCommands.importAccessMappings String",
                "Imports all ACL mappings from a given file.");
        command.add(com);
        com = new MCRCommand("export entity {0} to file {1}", "org.mycore.backend.hibernate.MCRHIBCtrlCommands.exportEntity String String",
                "Exports an entity (fully qualified class name) to a given file.");
        command.add(com);
        com = new MCRCommand("import entity {0} from file {1}", "org.mycore.backend.hibernate.MCRHIBCtrlCommands.importEntity String String",
                "Imports an entity (fully qualified class name) from a given file.");
        command.add(com);
    }

    /**
     * 
     * method creates tables using hibernate
     */
    public static void createTables() {
        try {
            new SchemaUpdate(MCRHIBConnection.instance().getConfiguration()).execute(true, true);

            LOGGER.info("tables created.");
        } catch (MCRPersistenceException e) {
            throw new MCRException("error while creating tables.", e);
        } catch (HibernateException e) {
            throw new MCRException("Hibernate error while creating database tables.", e);
        }
    }

    public static void exportAccessRules(String file) throws IOException, SAXException {
        exportFile(file, "accessrules", MCRACCESSRULE.class);
    }

    public static void importAccessRules(String file) throws IOException, DocumentException {
        importFile(file, MCRACCESSRULE.class.getName());
    }

    public static void exportAccessMappings(String file) throws IOException, SAXException {
        exportFile(file, "accessmap", MCRACCESS.class);
    }

    public static void importAccessMappings(String file) throws IOException, DocumentException {
        importFile(file, MCRACCESS.class.getName());
    }

    public static void exportEntity(String className, String file) throws IOException, SAXException, ClassNotFoundException {
        Class<?> exportClass = Class.forName(className);
        exportFile(file, exportClass.getSimpleName().toLowerCase(), exportClass);
    }

    public static void importEntity(String className, String file) throws IOException, DocumentException {
        importFile(file, className);
    }

    @SuppressWarnings("unchecked")
    private static void exportFile(String file, String rootTag, Class persistedClass) throws FileNotFoundException, UnsupportedEncodingException, IOException,
            SAXException {
        File exportFile = new File(file);
        if (exportFile.exists() && exportFile.isDirectory()) {
            throw new MCRException(exportFile.getAbsolutePath() + " is a directory.");
        }
        Session session = MCRHIBConnection.instance().getSession();
        Session xmlSession = session.getSession(EntityMode.DOM4J);
        QName rootName = DOC_FACTORY.createQName(rootTag, "mycore", "http://www.mycore.org/");
        OutputFormat format = OutputFormat.createCompactFormat();
        format.setTrimText(false);
        format.setNewLineAfterDeclaration(true);
        format.setSuppressDeclaration(false);
        format.setEncoding("UTF-8");
        FileOutputStream fileOutputStream = new FileOutputStream(exportFile);
        BufferedOutputStream bout = new BufferedOutputStream(fileOutputStream);
        XMLWriter xmlWriter = new XMLWriter(bout, format);
        /*
         * We don't generate a Document instance here to save memory on large
         * tables. Instead we code our document by SAX events and directly write
         * every element generated by hibernate to the OutputStream.
         */
        xmlWriter.startDocument();
        xmlWriter.write(DOC_FACTORY.createComment("Export from: " + new Date().toString()));
        xmlWriter.startPrefixMapping(rootName.getNamespacePrefix(), rootName.getNamespaceURI());
        xmlWriter.startElement(rootName.getNamespaceURI(), rootName.getName(), rootName.getQualifiedName(), new AttributesImpl());
        // get all Objects of persistedClass from hibernate
        List<Element> elements = xmlSession.createCriteria(persistedClass).list();
        for (Element result : elements) {
            // write element to stream
            xmlWriter.write(result);
        }
        // close document
        xmlWriter.endElement(rootName.getNamespaceURI(), rootName.getName(), rootName.getQualifiedName());
        xmlWriter.endPrefixMapping(rootName.getNamespacePrefix());
        xmlWriter.endDocument();
        xmlWriter.close();
        bout.close();
    }

    @SuppressWarnings("unchecked")
    private static void importFile(String file, final String entityName) throws DocumentException {
        File importFile = new File(file);
        if (importFile.exists() && importFile.isDirectory()) {
            throw new MCRException(importFile.getAbsolutePath() + " is a directory.");
        }
        SAXReader xmlReader = new SAXReader(false);
        Session session = MCRHIBConnection.instance().getSession();
        final Session xmlSession = session.getSession(EntityMode.DOM4J);
        final ReplicationMode replicationMode = ReplicationMode.OVERWRITE;
        /*
         * The Document returned below is not of interest. We just care for the
         * elements directly below the root element. Directly after a element is
         * completely read, the element gets detached so it can be caught be the
         * garbage collector.
         */
        xmlReader.setDefaultHandler(new ElementHandler() {

            public void onStart(ElementPath path) {
            }

            public void onEnd(ElementPath path) {
                // only process elements directly below the root element
                if (OBJECT_PATTERN.matcher(path.getPath()).find()) {
                    Element rule = path.getCurrent();
                    // import into database
                    xmlSession.replicate(entityName, rule, replicationMode);
                    // save some memory
                    rule.detach();
                }
            }
        });
        xmlReader.read(importFile);
    }

}
