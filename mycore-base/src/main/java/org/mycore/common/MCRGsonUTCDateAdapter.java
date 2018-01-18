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

package org.mycore.common;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.bind.util.ISO8601Utils;

// https://stackoverflow.com/questions/26044881/java-date-to-utc-using-gson
public class MCRGsonUTCDateAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

    private final DateFormat dateFormat;

    private final DateFormat enUsFormat;

    private final DateFormat localFormat;

    public MCRGsonUTCDateAdapter() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);      //This is the format I need
        dateFormat.setTimeZone(TimeZone.getTimeZone(
            "UTC"));                               //This is the key line which converts the date to UTC which cannot be accessed with the default serializer
        enUsFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.US);
        localFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.ROOT);
    }

    @Override
    public synchronized JsonElement serialize(Date date, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(dateFormat.format(date));
    }

    @Override
    public synchronized Date deserialize(JsonElement json, Type type,
        JsonDeserializationContext jsonDeserializationContext) {

        try {
            return dateFormat.parse(json.getAsString());
        } catch (ParseException e) {
            // backward compatibility (parse like default GSON does)
            synchronized (localFormat) {
                try {
                    return localFormat.parse(json.getAsString());
                } catch (ParseException ignored) {
                }
                try {
                    return enUsFormat.parse(json.getAsString());
                } catch (ParseException ignored) {
                }
                try {
                    return ISO8601Utils.parse(json.getAsString(), new ParsePosition(0));
                } catch (ParseException e1) {
                    throw new JsonSyntaxException(json.getAsString(), e);
                }
            }
        }
    }
}
