/* 
 * $Revision: 34285 $ $Date: 2016-01-07 14:05:50 +0100 (Do, 07 Jan 2016) $
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 */
package org.mycore.restapi.v1.utils;

import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.mycore.common.config.MCRConfiguration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * This class contains some generic utility functions for the REST API
 * 
 * @author Robert Stephan
 */
public class MCRRestAPIUtil {
    
    private static Pattern ALLOWED_WRITE_IP_PATTERN = Pattern.compile(MCRConfiguration.instance().getString("MCR.RestAPI.v1.Filter.Write.IPs.Pattern"));
    
    /**
     * checks wether the IP of the client is an allowed IP for Upload
     * @param request
     * @return
     */
    public static Response checkWriteAccessForIP(HttpServletRequest request) {
        String clientIP = getClientIpAddress(request);
        if(ALLOWED_WRITE_IP_PATTERN.matcher(clientIP).matches()){
            return Response.ok().build();
        }
        else{
           return createErrorResponse(Status.FORBIDDEN,  "ACCESS_DENIED", "The client IP "+clientIP+" is not allowed to write to the Rest API", null);
        }
    }
    
    /**
     * returns the client IP-Address from HTTP request
     * 
     * @param request
     * @return
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        } else {
            // see https://en.wikipedia.org/wiki/X-Forwarded-For
            // the header may contain a list of IPs, we only need the client (first IP)
            return new StringTokenizer(xForwardedForHeader, ",").nextToken().trim();
        }
    }
    
    /**
     * creates an error response
     * 
     * the format is specified in: http://jsonapi.org/format/#errors
     * 
     * @param status - the HTTP status code
     * @param code - an application specific error code
     * @param title - the error title
     * @param detail - further details - may be null
     * @return
     */
    public static Response createErrorResponse(Status status, String code, String title, String detail){
        JsonObject error = new JsonObject();
        error.addProperty("status", String.valueOf(status.getStatusCode()));
        error.addProperty("code",  code);
        error.addProperty("title",  title);
        if(detail!=null){
            error.addProperty("detail", detail);
        }
        JsonArray errors = new JsonArray();
        errors.add(error);
        JsonObject errorMsg = new JsonObject();
        errorMsg.add("errors", errors);
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return Response.status(status).entity(gson.toJson(errorMsg)).build();
    }
}
