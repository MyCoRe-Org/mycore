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

package org.mycore.pi.purl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * PURL Manager to register Persistent URLs on a PURL server
 *
 * for further documentation see PURLZ Wiki:
 * https://code.google.com/archive/p/persistenturls/wikis
 *
 * Hint:
 * -----
 * Please check in your code that you do not register / override regular PURLs in test / development
 * by checking: 
 *    if (resolvingURL.contains("localhost")) {
 *          purl = "/test" + purl;
 *    }
 *
 * @author Robert Stephan
 *
 */
public class MCRPURLManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String UTF_8_STR = "UTF-8";

    private static final String ADMIN_PATH = "/admin";

    private static final String PURL_PATH = ADMIN_PATH + "/purl";

    private static final String COOKIE_HEADER_PARAM = "Cookie";

    private final Charset UTF_8 = Charset.forName(UTF_8_STR);

    private String purlServerBaseURL;

    private String cookie = null;

    /**
     * sets the session cookie, if the login was successful
     *
     * @param purlServerURL
     *            - the base URL of the PURL server
     * @param user
     *            - the PURL server user
     * @param password
     *            - the user's password
     */
    public void login(String purlServerURL, String user, String password) {
        HttpURLConnection conn = null;
        try {
            purlServerBaseURL = purlServerURL;
            // Get Cookie
            URL url = new URL(purlServerBaseURL + ADMIN_PATH + "/login/login.bsh?referrer=/docs/index.html");
            conn = (HttpURLConnection) url.openConnection();
            conn.connect();

            String headerName;
            for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
                if ("Set-Cookie".equals(headerName)) {
                    cookie = conn.getHeaderField(i);
                    LOGGER.debug("Cookie: " + cookie);
                }
            }
            conn.disconnect();

            // Login
            String data = "id=" + URLEncoder.encode(user, UTF_8_STR);
            data += "&passwd=" + URLEncoder.encode(password, UTF_8_STR);

            url = new URL(purlServerBaseURL + ADMIN_PATH + "/login/login-submit.bsh");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty(COOKIE_HEADER_PARAM, cookie);

            conn.setDoOutput(true);
            try (OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), UTF_8)) {
                wr.write(data);
                wr.flush();
                LOGGER.error(url.toString() + " -> " + conn.getResponseCode());

                // Get the response
                try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), UTF_8))) {

                    String line;
                    while ((line = rd.readLine()) != null) {
                        if ("PURL User Login Failure".equals(line.trim())) {
                            cookie = null;
                            break;
                        }

                    }
                }
            }
            conn.disconnect();

        } catch (IOException e) {
            if (!e.getMessage().contains(
                "Server returned HTTP response code: 403 for URL: ")) {
                LOGGER.error(e);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * logout from PURL server
     */
    public void logout() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(purlServerBaseURL + ADMIN_PATH + "/logout?referrer=/docs/index.html");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty(COOKIE_HEADER_PARAM, cookie);

            conn.setDoOutput(true);
            try (OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), UTF_8)) {
                wr.flush();
            }
            LOGGER.debug(url.toString() + " -> " + conn.getResponseCode());
        } catch (IOException e) {
            if (!e.getMessage().contains(
                "Server returned HTTP response code: 403 for URL: ")) {
                LOGGER.error(e);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    // returns the response code

    /**
     * register a new PURL
     * @param purl - the PURL
     * @param target the target URL
     * @param type - the PURL type
     * @param maintainers - the maintainers
     * @return the HTTP Status Code of the request
     */
    public int registerNewPURL(String purl, String target, String type, String maintainers) {
        int response = 0;
        HttpURLConnection conn = null;
        try {
            // opener.open("http://localhost:8080/admin/purl/net/test2",
            // urllib.urlencode(dict(type="410", maintainers="admin"))).read().close() #
            // Create a 410 purl

            URL url = new URL(purlServerBaseURL + PURL_PATH + purl);
            LOGGER.debug(url.toString());

            String data = "target=" + URLEncoder.encode(target, UTF_8_STR);
            data += "&maintainers=" + maintainers;
            data += "&type=" + type;

            LOGGER.debug(data);

            // Send data

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty(COOKIE_HEADER_PARAM, cookie);
            conn.setRequestMethod("POST");

            conn.setDoOutput(true);

            try (OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), UTF_8)) {
                wr.write(data);
                wr.flush();
            }
            response = conn.getResponseCode();

            if (response != 200 && conn.getErrorStream() != null && LOGGER.isErrorEnabled()) {
                try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getErrorStream(), UTF_8))) {
                    String line;
                    while ((line = rd.readLine()) != null) {
                        LOGGER.error(line);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return response;
    }

    /**
     * updates an existing PURL
     *
     * @param purl
     *            - the PURL (relative URL)
     * @param target
     *            - the target URL
     * @param type
     *            - the PURL type
     * @param maintainers
     *            list of maintainers (PURL server users or groups)
     * @return the HTTP Status Code of the request
     */
    public int updateExistingPURL(String purl, String target, String type, String maintainers) {
        int response = 0;
        HttpURLConnection conn = null;
        try {
            // opener.open("http://localhost:8080/admin/purl/net/test2",
            // urllib.urlencode(dict(type="410", maintainers="admin"))).read().close() #
            // Create a 410 purl

            String strURL = purlServerBaseURL + PURL_PATH + purl;
            strURL +=
                "?target=" + URLEncoder.encode(target, UTF_8_STR) + "&maintainers=" + maintainers + "&type=" + type;

            URL url = new URL(strURL);
            LOGGER.debug(url.toString());

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty(COOKIE_HEADER_PARAM, cookie);
            conn.setRequestMethod("PUT");
            response = conn.getResponseCode();

            if (response != 200 && conn.getErrorStream() != null && LOGGER.isErrorEnabled()) {
                try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getErrorStream(), UTF_8))) {
                    String line = null;
                    while ((line = rd.readLine()) != null) {
                        LOGGER.error(line);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return response;
    }

    /**
     * deletes an existing PURL
     *
     * @param purl
     * @return the HTTP Status Code of the request
     */
    public int deletePURL(String purl) {
        int response = 0;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(purlServerBaseURL + PURL_PATH + purl);
            LOGGER.debug(url.toString());

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty(COOKIE_HEADER_PARAM, cookie);
            conn.setRequestMethod("DELETE");
            response = conn.getResponseCode();

            if (response != 200 || conn.getErrorStream() != null && LOGGER.isErrorEnabled()) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getErrorStream(), UTF_8));
                String line = null;
                while ((line = rd.readLine()) != null) {
                    LOGGER.error(line);
                }
                rd.close();
            }
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return response;
    }

    /**
     * check if a purl has the given target url
     * @param purl - the purl
     * @param targetURL - the target URL
     * @return true, if the target URL is registered at the given PURL
     */
    public boolean isPURLTargetURLUnchanged(String purl, String targetURL) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(purlServerBaseURL + PURL_PATH + purl);
            conn = (HttpURLConnection) url.openConnection();
            int response = conn.getResponseCode();

            if (response == 200) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(conn.getInputStream());
                /*
                 * <purl status="1"> <id>/test/rosdok/ppn750527188</id> <type>302</type>
                 * <maintainers><uid>rosdok</uid><uid>test</uid></maintainers>
                 * <target><url>http://localhost:8080/rosdok/resolve/id/
                 * rosdok_document_0000000259</url></target> </purl>
                 */
                Element eTarget = (Element) doc.getDocumentElement().getElementsByTagName("target").item(0);
                Element eTargetUrl = (Element) eTarget.getElementsByTagName("url").item(0);
                return targetURL.equals(eTargetUrl.getTextContent().trim());
            }
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return false;
    }
}
