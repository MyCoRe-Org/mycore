/*
 * $Id$
 * $Revision: 5697 $ $Date: 06.09.2010 $
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

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.jdom2.JDOMException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * replacement for <code>org.hsqldb.util.ShutdownServer</code> which is missing
 * in hsqldb 2.0
 * 
 * @author Thomas Scheffler (yagee)
 * 
 */
public class MCRShutdownServer {

    private static final ClassLoader CLASS_LOADER = MCRShutdownServer.class.getClassLoader();

    /**
     * @param args
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws JDOMException
     * @throws IOException
     * @throws SAXException 
     */
    public static void main(String[] args) throws IOException, SAXException {
        Map<String, String> properties = getConnectionProperties();
        String dbURL = properties.get("connection.url");
        String user = properties.get("connection.user");
        String pwd = properties.get("connection.password");
        try (Connection con = DriverManager.getConnection(dbURL, user, pwd);
            Statement statement = con.createStatement()) {
            statement.execute("SHUTDOWN");
        } catch (SQLException e) {
            if (e.getErrorCode() == -1305 && "08006".equals(e.getSQLState())) {
                //ignore EOF Exception on closing connection, database shutdown to fast
            } else {
                System.err.printf("Error while shutting down HSQLDB.\nCode: %d\nState: %s\nMessage: %s\n",
                    e.getErrorCode(), e.getSQLState(), e.getMessage());
            }
        }
    }

    private static Map<String, String> getConnectionProperties() throws IOException, SAXException {
        URL hibernateCfg = CLASS_LOADER.getResource("hibernate.cfg.xml");
        if (hibernateCfg == null) {
            throw new IOException("Could not find 'hibernate.cfg.xml'");
        }
        PropertyHandler propertyHandler = new PropertyHandler(XMLReaderFactory.createXMLReader());
        propertyHandler.parse(hibernateCfg.toString());
        return propertyHandler.getProperties();
    }

    private static class PropertyHandler extends XMLFilterImpl {
        String property;

        StringBuilder value;

        HashMap<String, String> properties;

        public PropertyHandler(XMLReader parent) {
            super(parent);
            this.property = null;
            this.value = new StringBuilder();
            this.properties = new HashMap<>();
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            if (localName.equals("property")) {
                property = atts.getValue("", "name");
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (localName.equals("property")) {
                if (value.length() == 0) {
                    //System.err.println(System.currentTimeMillis() + " No value found for property: " + property);
                } else {
                    properties.put(property, value.toString().trim());
                }
                property = null;
                value.setLength(0);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (property != null) {
                value.append(ch, start, length);
            }
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            //do not resolve DTD
            return new InputSource(new StringReader(""));
        }
    }

}
