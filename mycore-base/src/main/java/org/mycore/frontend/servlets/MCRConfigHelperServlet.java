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

package org.mycore.frontend.servlets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servlet to map the properties defined in the init param "Properties" to a JSON object with the property name as key
 * and the property value as value. The properties in the init param need to be comma separated and can end with * to
 * match all properties which start with the string before *.
 */
public class MCRConfigHelperServlet extends HttpServlet {

    public static final String PROPERTIES_INIT_PARAM = "Properties";

    private Date lastChange;
    private String resultJson;

    @Override
    public void init() throws ServletException {
        super.init();

        updateJSON();

        MCRConfiguration2.addPropertyChangeEventLister((changedProperty) -> {
            String propertiesParameter = getPropertiesParameter();
            for (String property : propertiesParameter.split(",")) {
                if (property.endsWith("*")) {
                    String prefix = property.substring(0, property.length() - 1);
                    if (changedProperty.startsWith(prefix)) {
                        return true;
                    }
                } else {
                    if (property.equals(changedProperty)) {
                        return true;
                    }
                }
            }
            return false;
        }, (property, before, after) -> {
            try {
                updateJSON();
            } catch (ServletException e) {
                throw new MCRException(e);
            }
        });
    }

    private void updateJSON() throws ServletException {
        lastChange = new Date();
        try {
            resultJson = createResultJson();
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    private String createResultJson() throws IOException {
        Map<String, String> propertiesMap = new LinkedHashMap<>();
        String properties = getPropertiesParameter();
        for (String property : properties.split(",")) {
            if (property.endsWith("*")) {
                String prefix = property.substring(0, property.length() - 1);
                MCRConfiguration2.getSubPropertiesMap(prefix)
                    .forEach((key, value) -> propertiesMap.put(prefix + key, value));
            } else {
                MCRConfiguration2.getString(property).ifPresent((p) -> propertiesMap.put(property, p));
            }
        }

        JsonObject obj = new JsonObject();
        propertiesMap.forEach(obj::addProperty);

        try (StringWriter sw = new StringWriter()) {
            new Gson().toJson(obj, sw);
            return sw.toString();
        }
    }

    private String getPropertiesParameter() {
        String properties = getInitParameter(PROPERTIES_INIT_PARAM);

        if (properties == null) {
            throw new MCRConfigurationException(
                "The Servlet does not have the init Parameter '" + PROPERTIES_INIT_PARAM + "'");
        }
        return properties;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Cache-Control", "public");
        resp.setHeader("Cache-Control", "max-age=120");

        if (req.getDateHeader("If-Modified-Since") > lastChange.getTime()) {
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        } else {
            resp.setDateHeader("Last-Modified", lastChange.getTime());
            try (ServletOutputStream os = resp.getOutputStream()) {
                os.print(resultJson);
            }
        }
    }

    @Override
    protected long getLastModified(HttpServletRequest request) {
        return this.lastChange.getTime();
    }
}
