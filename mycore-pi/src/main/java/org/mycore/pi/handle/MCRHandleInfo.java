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

package org.mycore.pi.handle;

public class MCRHandleInfo {

    private Integer idx;

    private String type;

    private String data;

    private String timestamp;

    @SuppressWarnings({"checkstyle:membername"}) // this is required because the json field is named parsed_data
    private Object parsed_data;

    public MCRHandleInfo() {
    }

    public Integer getIdx() {
        return idx;
    }

    public void setIdx(Integer idx) {
        this.idx = idx;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @SuppressWarnings({"checkstyle:methodname"})
    public Object getParsed_data() {
        return parsed_data;
    }

    @SuppressWarnings({"checkstyle:methodname"})
    public void setParsed_data(Object parsedData) {
        this.parsed_data = parsedData;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "MCRHandleInfo{" +
            "idx=" + idx +
            ", type='" + type + '\'' +
            ", data='" + data + '\'' +
            ", timestamp='" + timestamp + '\'' +
            '}';
    }
}
