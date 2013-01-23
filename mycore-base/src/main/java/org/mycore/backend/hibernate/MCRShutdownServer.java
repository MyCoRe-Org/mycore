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
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 * replacement for <code>org.hsqldb.util.ShutdownServer</code> which is missing in hsqldb 2.0
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRShutdownServer {

    /**
     * @param args
     * @throws SQLException 
     * @throws ClassNotFoundException 
     * @throws JDOMException 
     * @throws IOException 
     */
    public static void main(String[] args) throws SQLException, ClassNotFoundException, IOException, JDOMException {
        Properties properties = getConnectionProperties();
        String dbURL = properties.getProperty("connection.url");
        String dbDriver = properties.getProperty("connection.driver_class");
        String user = properties.getProperty("connection.user");
        String pwd = properties.getProperty("connection.password");
        Class.forName(dbDriver);
        Connection con = DriverManager.getConnection(dbURL, user, pwd);
        Statement statement = con.createStatement();
        try {
            statement.executeUpdate("SHUTDOWN");
        } finally {
            statement.close();
            con.close();
        }
    }

    private static Properties getConnectionProperties() throws IOException, JDOMException {
        URL hibernateCfg = MCRShutdownServer.class.getClassLoader().getResource("hibernate.cfg.xml");
        URLConnection con = null;
        Document document;
        try {
            con = hibernateCfg.openConnection();
            SAXBuilder builder = new SAXBuilder();
            builder.setValidation(false);
            document = builder.build(con.getInputStream());
        } finally {
            if (con != null)
                con.getInputStream().close();
        }
        Properties prop = new Properties();
        @SuppressWarnings({ "unchecked" })
        List<Element> children = document.getRootElement().getChild("session-factory").getChildren("property");
        for (Element p : children) {
            prop.setProperty(p.getAttributeValue("name"), p.getTextTrim());
        }
        return prop;
    }

}
