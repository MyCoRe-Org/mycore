package org.mycore.pi.doi.rest;

import java.util.List;

public class MCRDOIRestResponse {
    int responseCode;

    String handle;

    List<MCRDOIRestResponseEntry> values;

    /**
     * 1 : Success. (HTTP 200 OK)
     * 2 : Error. Something unexpected went wrong during handle resolution. (HTTP 500 Internal Server Error)
     * 100 : Handle Not Found. (HTTP 404 Not Found)
     * 200 : Values Not Found. The handle exists but has no values (or no values according to the types and indices specified). (HTTP 200 OK)
     *
     * @return the code
     */
    public int getResponseCode() {
        return responseCode;
    }

    public String getHandle() {
        return handle;
    }

    public List<MCRDOIRestResponseEntry> getValues() {
        return values;
    }
}
