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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

import org.mycore.common.MCRJSONUtils;

import com.google.gson.JsonObject;

public class MCRCommandListProcessor extends SubmissionPublisher<JsonObject>
    implements Flow.Processor<List<String>, JsonObject> {
    private Flow.Subscription upstreamSubscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.upstreamSubscription = subscription;
        upstreamSubscription.request(1);
    }

    @Override
    public void onNext(List<String> item) {
        try {
            ArrayList<String> copy;
            synchronized (item) {
                copy = new ArrayList<>(item);
            }
            JsonObject jObject = new JsonObject();
            jObject.addProperty("type", "commandQueue");
            jObject.add("return", MCRJSONUtils
                .getJsonArray(copy.subList(0, Math.min(copy.size(), 100))));
            jObject.addProperty("size", copy.size());
            submit(jObject);
        } finally {
            upstreamSubscription.request(1);
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
        upstreamSubscription.cancel();
        super.close();
    }
}
