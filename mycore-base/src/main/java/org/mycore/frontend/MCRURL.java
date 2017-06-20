package org.mycore.frontend;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 * @author Matthias Eichner
 */
public class MCRURL {

    static Logger LOGGER = LogManager.getLogger(MCRURL.class);

    private URL url;

    private Map<String, List<String>> parameterMap;

    public MCRURL(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    public Map<String, List<String>> getParameterMap() {
        if (this.parameterMap != null) {
            return parameterMap;
        }
        return this.parameterMap = buildParameterMap(this.url);
    }

    private Map<String, List<String>> buildParameterMap(URL url) {
        Map<String, List<String>> p = new HashMap<String, List<String>>();
        String queryString = url.getQuery();
        if (queryString == null) {
            return p;
        }
        for (String pair : queryString.split("&")) {
            int eq = pair.indexOf("=");
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            String value = eq >= 0 ? pair.substring(eq + 1) : "";
            if (p.containsKey(key)) {
                p.get(key).add(value);
            } else {
                List<String> valueList = new ArrayList<>();
                valueList.add(value);
                p.put(key, valueList);
            }
        }
        return p;
    }

    private String buildQueryString(Map<String, List<String>> parameterMap) {
        StringBuilder queryBuilder = new StringBuilder();
        for (Entry<String, List<String>> entrySet : parameterMap.entrySet()) {
            String name = entrySet.getKey();
            for (String value : entrySet.getValue()) {
                queryBuilder.append(name).append("=").append(value).append("&");
            }
        }
        String queryString = queryBuilder.toString();
        return queryString.length() > 0 ? queryString.substring(0, queryString.length() - 1) : "";
    }

    private void rebuild() {
        String query = buildQueryString(this.parameterMap);
        try {
            URI uri = this.url.toURI();
            StringBuffer urlBuffer = new StringBuffer();
            urlBuffer.append(
                new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), null, null)
                    .toString());
            if (query != null) {
                urlBuffer.append("?").append(query);
            }
            if (uri.getFragment() != null) {
                urlBuffer.append("#").append(uri.getFragment());
            }
            this.url = new URL(urlBuffer.toString());
            if (this.parameterMap != null) {
                // rebuild parameter map
                this.parameterMap = buildParameterMap(this.url);
            }
        } catch (Exception exc) {
            LOGGER.error("unable to rebuild url " + this.url.toString());
        }
    }

    public String getParameter(String name) {
        List<String> valueList = getParameterMap().get(name);
        return valueList != null ? valueList.get(0) : null;
    }

    public List<String> getParameterValues(String name) {
        List<String> valueList = getParameterMap().get(name);
        return valueList != null ? valueList : new ArrayList<String>();
    }

    public MCRURL addParameter(String name, String value) {
        StringBuilder urlBuffer = new StringBuilder(this.url.toString());
        urlBuffer.append(this.url.getQuery() == null ? "?" : "&");
        urlBuffer.append(name).append("=").append(value);
        try {
            this.url = new URL(urlBuffer.toString());
            if (this.parameterMap != null) {
                // rebuild parameter map
                this.parameterMap = buildParameterMap(this.url);
            }
        } catch (MalformedURLException exc) {
            LOGGER.error("unable to add parameter (" + name + "=" + value + ") to url" + this.url.toString());
        }
        return this;
    }

    public MCRURL removeParameter(String name) {
        this.getParameterMap().remove(name);
        rebuild();
        return this;
    }

    public MCRURL removeParameterValue(String name, String value) {
        List<String> values = this.getParameterMap().get(name);
        if (values != null) {
            boolean removed = false;
            while (values.remove(value)) {
                removed = true;
            }
            if (removed) {
                rebuild();
            }
        }
        return this;
    }

    public URL getURL() {
        return this.url;
    }

    @Override
    public String toString() {
        return this.url.toString();
    }

}
