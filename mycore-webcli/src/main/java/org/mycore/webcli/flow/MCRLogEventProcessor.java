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

package org.mycore.webcli.flow;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

import org.apache.logging.log4j.core.LogEvent;

import com.google.gson.JsonObject;

public class MCRLogEventProcessor extends SubmissionPublisher<JsonObject>
    implements Flow.Processor<LogEvent, JsonObject> {

    private Flow.Subscription upstreamSubscrition;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.upstreamSubscrition = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(LogEvent event) {
        try {
            JsonObject logEvent = new JsonObject();
            logEvent.addProperty("logLevel", event.getLevel().toString());
            logEvent.addProperty("message", event.getMessage().getFormattedMessage());
            String exception = null;
            if (event.getThrownProxy() != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                event.getThrownProxy().getThrowable().printStackTrace(pw);
                pw.close();
                exception = sw.toString();
            }
            logEvent.addProperty("exception", exception);
            logEvent.addProperty("time", event.getTimeMillis());
            JsonObject json = new JsonObject();
            json.addProperty("type", "log");
            json.add("return", logEvent);
            submit(json);
        } finally {
            upstreamSubscrition.request(1);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        super.closeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        super.close();
    }

    @Override
    public void close() {
        this.upstreamSubscrition.cancel();
        super.close();
    }
}
