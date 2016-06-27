package org.mycore.pi.frontend.model;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MCRPIErrorJSON {


    public String message;
    public String stackTrace;

    public MCRPIErrorJSON(String message) {
        this(message, null);
    }

    public MCRPIErrorJSON(String message, Exception e) {
        this.message = message;

        if (e != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            stackTrace = sw.toString();
        }
    }
}
