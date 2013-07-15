package org.mycore.frontend;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Matthias Eichner
 */
public class MCRURL {

    private URL url;
    private Map<String, List<String>> parameterMap;

    public MCRURL(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    public Map<String, List<String>> getParameterMap() {
        if(this.parameterMap != null) {
            return parameterMap;
        }
        Map<String, List<String>> p = new HashMap<String, List<String>>();
        String queryString = url.getQuery();
        for(String pair : queryString.split("&")) {
            int eq = pair.indexOf("=");
            if(eq < 0) {
                continue;
            }
            String key = pair.substring(0, eq);
            String value = pair.substring(eq + 1);
            if(p.containsKey(key)) {
                p.get(key).add(value);
            } else {
                List<String> valueList= new ArrayList<>();
                valueList.add(value);
                p.put(key, valueList);
            }
        }
        return this.parameterMap = p;
    }

    public String getParameter(String name) {
        List<String> valueList = getParameterMap().get(name);
        return valueList != null ? valueList.get(0) : null;
    }

    public URL getURL() {
        return this.url;
    }

}
