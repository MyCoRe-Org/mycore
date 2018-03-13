/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
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
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * replacement for <code>org.hsqldb.util.ShutdownServer</code> which is missing
 * in hsqldb 2.0
 * 
 * @author Thomas Scheffler (yagee)
 * 
 */
public class MCRShutdownServer {

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
        Map<String, String> properties = getConnectionProperties();
        String dbURL = properties.get("javax.persistence.jdbc.url");
        String user = properties.get("javax.persistence.jdbc.user");
        String pwd = properties.get("javax.persistence.jdbc.password");
        try (Connection con = DriverManager.getConnection(dbURL, user, pwd);
            Statement statement = con.createStatement()) {
            statement.execute("SHUTDOWN");
        } catch (SQLException e) {
            if (e.getErrorCode() == -1305 && "08006".equals(e.getSQLState())) {
                //ignore EOF Exception on closing connection, database shutdown to fast
            } else {
                System.err.printf(Locale.ROOT, "Error while shutting down HSQLDB.%nCode: %d%nState: %s%nMessage: %s%n",
                    e.getErrorCode(), e.getSQLState(), e.getMessage());
            }
        }
    }

    private static Map<String, String> getConnectionProperties()
        throws IOException, SAXException, ParserConfigurationException {
        String resourceName = "META-INF/persistence.xml";
        URL hibernateCfg = MCRConfigurationDir.getConfigResource(resourceName);
        if (hibernateCfg == null) {
            throw new IOException("Could not find '" + resourceName + "'");
        }
        PropertyHandler propertyHandler = new PropertyHandler(
            MCRXMLParserFactory.getNonValidatingParser().getXMLReader());
        propertyHandler.parse(hibernateCfg.toString());
        return propertyHandler.getProperties();
    }

    private static class PropertyHandler extends XMLFilterImpl {

        HashMap<String, String> properties;

        public PropertyHandler(XMLReader parent) {
            super(parent);
            this.properties = new HashMap<>();
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            if (localName.equals("property")) {
                String property = atts.getValue("", "name");
                String value = atts.getValue("", "value");
                properties.put(property, value);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            //do not resolve DTD
            return new InputSource(new StringReader(""));
        }
    }

}
