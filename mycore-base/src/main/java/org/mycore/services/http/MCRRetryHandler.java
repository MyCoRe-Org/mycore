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

    MCRRetryHandler(int maxExecutionCount) {
        super();
        this.maxExecutionCount = maxExecutionCount;
    }

    @Override
    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
        boolean result;
       // Do not retry if over max retry count,Unknown host
        if (executionCount >= maxExecutionCount||exception instanceof UnknownHostException) {
            result= false;
       // Timeout, Connection refused
        } else if(exception instanceof InterruptedIOException||exception instanceof ConnectException){
            result= true;
        }else{
            result= !(exception instanceof SSLException);
        }
        return result;
    }

}
