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

package org.mycore.mets.model.converter;

import java.io.IOException;

import org.mycore.mets.model.simple.MCRMetsLink;
import org.mycore.mets.model.simple.MCRMetsPage;
import org.mycore.mets.model.simple.MCRMetsSection;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * This is a helper class to help GSON to convert simple model to JSON.
 * @author Sebastian Hofmann(mcrshofm)
 */
public class MCRMetsLinkTypeAdapter extends TypeAdapter<MCRMetsLink> {

    @Override
    public void write(JsonWriter jsonWriter, MCRMetsLink metsLink) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name("from").value(metsLink.getFrom().getId());
        jsonWriter.name("to").value(metsLink.getTo().getId());
        jsonWriter.endObject();
    }

    @Override
    public MCRMetsLink read(JsonReader jsonReader) throws IOException {
        MCRMetsLinkPlaceholder ml = new MCRMetsLinkPlaceholder();

        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            switch (jsonReader.nextName()) {
                case "from":
                    ml.setFromString(jsonReader.nextString());
                    break;
                case "to":
                    ml.setToString(jsonReader.nextString());
                    break;
            }
        }
        jsonReader.endObject();

        return ml;
    }

    protected class MCRMetsLinkPlaceholder extends MCRMetsLink {
        public static final String PLACEHOLDER_EXCEPTION_MESSAGE = "this is a placeholder class";

        private String fromString;

        private String toString;

        public String getFromString() {
            return fromString;
        }

        public void setFromString(String fromString) {
            this.fromString = fromString;
        }

        public String getToString() {
            return toString;
        }

        public void setToString(String toString) {
            this.toString = toString;
        }

        @Override
        public MCRMetsPage getTo() {
            throw new RuntimeException(PLACEHOLDER_EXCEPTION_MESSAGE);
        }

        @Override
        public MCRMetsSection getFrom() {
            throw new RuntimeException(PLACEHOLDER_EXCEPTION_MESSAGE);
        }

        @Override
        public void setFrom(MCRMetsSection from) {
            throw new RuntimeException(PLACEHOLDER_EXCEPTION_MESSAGE);
        }

        @Override
        public void setTo(MCRMetsPage to) {
            throw new RuntimeException(PLACEHOLDER_EXCEPTION_MESSAGE);
        }
    }
}
