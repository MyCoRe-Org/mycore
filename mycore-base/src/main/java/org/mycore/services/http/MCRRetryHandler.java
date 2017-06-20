package org.mycore.services.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;

/**
 * Handles request retries. 
 * @author Thomas Scheffler (yagee)
 */
class MCRRetryHandler implements HttpRequestRetryHandler {
    int maxExecutionCount;

    public MCRRetryHandler(int maxExecutionCount) {
        super();
        this.maxExecutionCount = maxExecutionCount;
    }

    @Override
    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
        if (executionCount >= maxExecutionCount) {
            // Do not retry if over max retry count
            return false;
        }
        if (exception instanceof InterruptedIOException) {
            // Timeout
            return true;
        }
        if (exception instanceof UnknownHostException) {
            // Unknown host
            return false;
        }
        if (exception instanceof ConnectException) {
            // Connection refused
            return true;
        }
        if (exception instanceof SSLException) {
            // SSL handshake exception
            return false;
        }
        return true;
    }

}
